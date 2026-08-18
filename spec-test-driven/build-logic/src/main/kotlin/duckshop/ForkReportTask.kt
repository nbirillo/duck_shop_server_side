package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
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
        providers.gradleProperty("forkTests").orNull?.let { asked ->
            require(asked.trim('/') == ranSuite.trim('/')) {
                "-PforkTests=$asked but the modules were generated with '$ranSuite'. Re-run " +
                    "generateForks with the suite you mean; the report will not score one suite and " +
                    "name another."
            }
        }

        val verdicts = readings.groupBy { it.fork }.toSortedMap().map { (fork, group) ->
            val accepted = mutableListOf<Reading>()
            val rejected = mutableListOf<Reading>()
            val unbuilt = mutableListOf<Reading>()
            group.forEach { reading ->
                val failures = failingTests(root.resolve("$outDir/${reading.id}/build/test-results/test"))
                when {
                    // No results at all is a DIFFERENT fact from "nothing failed" — usually the
                    // reading did not compile against this suite. Reported as its own outcome, because
                    // an earlier report of mine folded it into "rejected" and read as a decision.
                    failures == null -> unbuilt += reading
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
            val word = when {
                v.unbuilt.isNotEmpty() -> "DID NOT BUILD"
                v.settled -> "SETTLED"
                v.contradictory -> "CONTRADICTORY"
                else -> "LEFT OPEN"
            }
            logger.lifecycle("${v.fork}: $word")
            v.unbuilt.forEach { logger.lifecycle("    did not build   ${it.id} — ${it.label}") }
            v.accepted.forEach { logger.lifecycle("    accepted        ${it.id} — ${it.label}") }
            v.rejected.forEach { logger.lifecycle("    rejected        ${it.id} — ${it.label}") }
            when {
                v.unbuilt.isNotEmpty() ->
                    logger.lifecycle("  ⇒ nothing is measured here until it compiles. Run with --continue.")
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
}
