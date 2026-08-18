package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * The capstone's report: for each fork the brief leaves open, does the suite under test **decide** it?
 *
 * See [Forks] for why this replaced the hidden corner-case set. There is deliberately no score and no
 * right answer — a fork is either decided or it is not, and which way it was decided is the learner's
 * to defend.
 */
abstract class ForkReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    abstract val catalogPath: Property<String>

    @get:Input
    abstract val outPath: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val outDir = outPath.get().trim('/')
        val readings = loadForkCatalog(root.resolve(catalogPath.get().trim('/')))

        // Which suite really ran. See the note in GenerateForksTask: the suite is baked into the
        // generated modules, so a -PforkTests that disagrees with them would produce a report about a
        // suite nobody ran — invisible unless you read the failures, which is how this fault family
        // wasted three earlier measurements.
        val marker = root.resolve("$outDir/generated-with.txt")
        require(marker.isFile) {
            "No $outDir/generated-with.txt — run ./gradlew generateForks first (the readings' modules " +
                "hold the suite they were generated with, and the report will not guess)."
        }
        val generatedWith = marker.readLines()
            .mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 } }
            .associate { (k, v) -> k to v }
        val ranSuite = generatedWith["tests"].orEmpty()
        // A fork check with no suite compiled into the readings measures nothing, and it used to SAY
        // something instead: with no tests there are no result files, which the branch below reads as
        // "did not build". Refusing outright is the honest answer.
        require(ranSuite.isNotBlank()) {
            "The readings were generated with no test suite, so there is nothing to measure. Re-run " +
                "./gradlew generateForks -PforkTests=<your test source dir> first."
        }
        providers.gradleProperty("forkTests").orNull?.let { asked ->
            require(asked.trim('/') == ranSuite.trim('/')) {
                "-PforkTests=$asked but the modules were generated with '$ranSuite'. Re-run " +
                    "generateForks with the suite you mean; the report will not score one suite and " +
                    "name another."
            }
        }

        // A test claiming to hold on EVERY reading, that some reading rejects. See the block that
        // prints it: this is the invariant keeping a "settled" group honest, and it caught two tests of
        // mine that were settled by argument and not by measurement.
        val overreaching = sortedMapOf<String, MutableSet<String>>()

        /** Readings with no results whose test sources really did fail to compile. */
        val notCompiled = sortedSetOf<String>()

        val verdicts = readings.groupBy { it.fork }.toSortedMap().map { (fork, group) ->
            val accepted = mutableListOf<Reading>()
            val rejected = mutableListOf<Reading>()
            val unbuilt = mutableListOf<Reading>()
            group.forEach { reading ->
                val moduleDir = root.resolve("$outDir/${reading.id}")
                val failures = failingTests(moduleDir.resolve("build/test-results/test"))
                failures.orEmpty()
                    .filter { it.trimStart().startsWith(SETTLED_PREFIX) }
                    .forEach { overreaching.getOrPut(it) { sortedSetOf() } += reading.id }
                when {
                    // No results at all is a DIFFERENT fact from "nothing failed" — its own outcome,
                    // because an earlier report of mine folded it into "rejected" and it read as a
                    // decision. TWO causes reach here and they are not the same problem: compiled test
                    // classes with no results means the suite ran nothing, while no classes at all means
                    // it did not compile. Saying "did not build" for the first one is what this branch
                    // did on its first run against the capstone build, and it was simply wrong.
                    failures == null -> {
                        unbuilt += reading
                        if (!ranNothing(moduleDir)) notCompiled += reading.id
                    }
                    failures.isEmpty() -> accepted += reading
                    else -> rejected += reading
                }
            }
            ForkVerdict(fork, accepted, rejected, unbuilt)
        }

        logger.lifecycle("")
        logger.lifecycle("Fork discrimination — does this suite DECIDE what the brief left open?")
        logger.lifecycle("suite: ${ranSuite.ifEmpty { "(none — every reading ran with no tests at all)" }}")
        logger.lifecycle("")
        logger.lifecycle("A fork is SETTLED when the suite accepts exactly one reading of it. Accepting")
        logger.lifecycle("several is not a bug it failed to catch — it is a decision nobody made.")
        logger.lifecycle("Neither reading of a fork is wrong: the report never says which to pick.")

        verdicts.forEach { v ->
            logger.lifecycle("")
            val anyFailedToCompile = v.unbuilt.any { it.id in notCompiled }
            val word = when {
                anyFailedToCompile -> "DID NOT COMPILE"
                v.unbuilt.isNotEmpty() -> "RAN NO TESTS"
                v.settled -> "SETTLED"
                v.contradictory -> "CONTRADICTORY"
                else -> "LEFT OPEN"
            }
            logger.lifecycle("${v.fork}: $word")
            v.unbuilt.forEach {
                val why = if (it.id in notCompiled) "did not compile" else "ran no tests"
                logger.lifecycle("    ${why.padEnd(15)} ${it.id} — ${it.label}")
            }
            v.accepted.forEach { logger.lifecycle("    accepted        ${it.id} — ${it.label}") }
            v.rejected.forEach { logger.lifecycle("    rejected        ${it.id} — ${it.label}") }
            when {
                anyFailedToCompile ->
                    logger.lifecycle("  ⇒ your tests do not compile against this reading, so nothing is " +
                        "measured here.\n     Run with --continue and read the compiler output above.")
                v.unbuilt.isNotEmpty() ->
                    logger.lifecycle("  ⇒ the suite compiled but contains no tests, so nothing is measured " +
                        "here.")
                v.settled ->
                    logger.lifecycle("  ⇒ your tests require '${v.accepted.single().label}'. That is a decision, " +
                        "and you own it.")
                v.contradictory ->
                    logger.lifecycle("  ⇒ your tests reject EVERY reading held here. Either a test is wrong, or " +
                        "you decided\n     this fork some third way — worth saying which, because the report " +
                        "cannot tell.")
                else ->
                    logger.lifecycle("  ⇒ your tests accept ${v.accepted.size} readings that answer this " +
                        "differently, so the\n     specification has not said anything yet. Pick one and pin it.")
            }
        }

        // A test named "settled — …" asserts something the brief decides, so it must hold on EVERY
        // reading; if a legitimate reading rejects it, either the test over-reaches or the reading is
        // excluded by something already settled — and both are real findings about the BRIEF. Deciding
        // that boundary by argument got it wrong twice, so it is checked here instead.
        if (overreaching.isNotEmpty()) {
            logger.lifecycle("")
            logger.warn("⚠ ${overreaching.size} test(s) named '$SETTLED_PREFIX…' are rejected by a reading:")
            overreaching.forEach { (test, by) -> logger.warn("    $test\n        rejected by ${by.joinToString()}") }
            logger.warn("  ⇒ a settled fact holds on every reading. Either the test pins a choice and")
            logger.warn("     belongs with the open group, or that reading contradicts something already")
            logger.warn("     settled and does not belong in the catalog. Both are findings about the brief.")
        }

        val settled = verdicts.count { it.settled }
        val open = verdicts.count { it.leftOpen }
        val broken = verdicts.count { it.contradictory }
        val unbuilt = verdicts.count { it.unbuilt.isNotEmpty() }
        logger.lifecycle("")
        logger.lifecycle(
            "$settled settled · $open left open · $broken contradictory · $unbuilt did not build " +
                "(of ${verdicts.size} forks)",
        )
        if (open > 0) {
            logger.lifecycle("")
            logger.lifecycle("A fork left open is not a mark against you until you SHIP it — but the answer names")
            logger.lifecycle("a shop and a price, so someone downstream will depend on whichever way your code")
            logger.lifecycle("happens to go. That is what deciding it in writing prevents.")
        }

        // Same opt-in as -PmutantsStrict: naming the flag is what makes an open fork a build failure,
        // so the report is always readable first.
        if (providers.gradleProperty("forksStrict").isPresent && (open > 0 || broken > 0 || unbuilt > 0)) {
            error("-PforksStrict: $open fork(s) left open, $broken contradictory, $unbuilt did not build")
        }
    }

    /**
     * True when this module's test sources compiled but produced no results — i.e. the suite is empty,
     * as opposed to broken. Compiled classes on disk with no JUnit XML beside them is the only signal
     * that separates the two, and telling a learner "did not build" when their suite merely has no
     * tests sends them looking for a compiler error that does not exist.
     */
    private fun ranNothing(moduleDir: File): Boolean {
        val classes = moduleDir.resolve("build/classes/kotlin/test")
        return classes.isDirectory && classes.walkTopDown().any { f -> f.extension == "class" }
    }

    private companion object {
        /**
         * Test-name prefix marking a claim that must hold on every reading. A naming convention rather
         * than a catalog field on purpose: the suite under test is not ours to add fields to, and the
         * same trick already keeps `scoreExtraction` honest about the key's claim list.
         */
        const val SETTLED_PREFIX = "settled"
    }
}
