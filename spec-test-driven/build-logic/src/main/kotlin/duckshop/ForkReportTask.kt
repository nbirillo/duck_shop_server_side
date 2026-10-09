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
 * Checks whether the capstone tests make a clear choice for each question the brief leaves open.
 *
 * See [Forks] for why we use this instead of hidden corner cases. There is no score
 * and no single correct answer. The report shows which choices the tests make;
 * the learner decides which choice is right for their specification.
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

        // Check which suite the generated modules actually use. If -PforkTests points
        // somewhere else, the report would describe tests that never ran. We've seen
        // this give misleading results before, so don't guess.
        val marker = root.resolve("$outDir/generated-with.txt")
        require(marker.isFile) {
            "Missing $outDir/generated-with.txt. Run ./gradlew generateForks first. The generated modules " +
                    "keep their test suite, so the report needs that information."
        }
        val generatedWith = marker.readLines()
            .mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 } }
            .associate { (k, v) -> k to v }
        val ranSuite = generatedWith["tests"].orEmpty()
        // No test suite means there's nothing to check. Stop here rather than
        // mistaking missing results for a build failure.
        require(ranSuite.isNotBlank()) {
            "These readings have no test suite, so there is nothing to check. Run " +
                    "./gradlew generateForks -PforkTests=<your test source dir> first."
        }
        providers.gradleProperty("forkTests").orNull?.let { asked ->
            require(asked.trim('/') == ranSuite.trim('/')) {
                "-PforkTests=$asked but the modules use '$ranSuite'. Re-run " +
                        "generateForks with the intended suite. The report cannot check one suite and " +
                        "report another."
            }
        }

        // Track tests marked "settled" that fail on a valid reading. A settled rule
        // should hold for every reading, so these failures need another look.
        val overreaching = sortedMapOf<String, MutableSet<String>>()

        /** Readings where the test code did not compile. */
        val notCompiled = sortedSetOf<String>()

        // An empty test folder is normal before the learner writes tests.
        // Check for source files so we don't report a compilation error by mistake.
        val suiteHasSources = root.resolve(ranSuite).let { dir ->
            dir.isDirectory && dir.walkTopDown().any { it.isFile && it.extension == "kt" }
        }

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
                    // No test results does not mean the reading was rejected.
                    // It may mean the tests never ran or failed to compile.
                    // Keep those cases separate from actual test failures.
                    failures == null -> {
                        unbuilt += reading
                        if (suiteHasSources && !ranNothing(moduleDir)) notCompiled += reading.id
                    }
                    failures.isEmpty() -> accepted += reading
                    else -> rejected += reading
                }
            }
            ForkVerdict(fork, accepted, rejected, unbuilt)
        }

        logger.lifecycle("")
        logger.lifecycle("Open choices — do your tests make a decision?")
        logger.lifecycle("suite: ${ranSuite.ifEmpty { "(none — every reading ran with no tests at all)" }}")
        logger.lifecycle("")
        logger.lifecycle("A fork is SETTLED when your tests accept exactly one reading. If they accept")
        logger.lifecycle("several, the choice is still open. That is not an implementation bug.")
        logger.lifecycle("Both readings are valid. This report does not choose one for you.")

        verdicts.forEach { v ->
            logger.lifecycle("")
            val anyFailedToCompile = v.unbuilt.any { it.id in notCompiled }
            val word = when {
                anyFailedToCompile -> "DID NOT COMPILE"
                v.unbuilt.isNotEmpty() && !suiteHasSources -> "NO TESTS YET"
                v.unbuilt.isNotEmpty() -> "RAN NO TESTS"
                v.settled -> "SETTLED"
                v.contradictory -> "CONTRADICTORY"
                else -> "LEFT OPEN"
            }
            logger.lifecycle("${v.fork}: $word")
            v.unbuilt.forEach {
                val why = when {
                    it.id in notCompiled -> "did not compile"
                    !suiteHasSources -> "no tests yet"
                    else -> "ran no tests"
                }
                logger.lifecycle("    ${why.padEnd(15)} ${it.id} — ${it.label}")
            }
            v.accepted.forEach { logger.lifecycle("    accepted        ${it.id} — ${it.label}") }
            v.rejected.forEach { logger.lifecycle("    rejected        ${it.id} — ${it.label}") }
            when {
                anyFailedToCompile ->
                    logger.lifecycle("  ⇒ your tests did not compile for this reading, so nothing was " +
                            "checked.\n     Run with --continue and look at the compiler errors above.")
                v.unbuilt.isNotEmpty() && !suiteHasSources ->
                    logger.lifecycle("  ⇒ $ranSuite has no tests yet, so there is nothing to " +
                            "check.\n     Add a test that chooses between these readings, " +
                            "then run this again.")
                v.unbuilt.isNotEmpty() ->
                    logger.lifecycle("  ⇒ the suite compiled but ran no tests, so nothing was checked " +
                            "here.")
                v.settled ->
                    logger.lifecycle("  ⇒ your tests choose '${v.accepted.single().label}' . That is your choice, " +
                            "and your tests now enforce it.")
                v.contradictory ->
                    logger.lifecycle("  ⇒ your tests reject every reading here. A test may be wrong, or " +
                            "you may have chosen a third option.\n     Explain which, because this report " +
                            "cannot decide.")
                else ->
                    logger.lifecycle("  ⇒ your tests still accept ${v.accepted.size} readings that handle this " +
                            "differently.\n     Choose the behavior you want and write a test for it.")
            }
        }

        // A test starting with "settled" claims something the brief already requires.
        // Every valid reading should pass it. If one doesn't, either the test is
        // too strict or that reading shouldn't be in the catalog.
        if (overreaching.isNotEmpty()) {
            logger.lifecycle("")
            logger.warn("⚠ ${overreaching.size} test(s) named '$SETTLED_PREFIX…' fail on a valid reading:")
            overreaching.forEach { (test, by) -> logger.warn("    $test\n        rejected by ${by.joinToString()}") }
            logger.warn("  ⇒ A settled requirement should hold for every reading. Either the test")
            logger.warn("     makes an extra choice, or the reading conflicts with something already")
            logger.warn("     settled and should not be in the catalog. Check the brief to decide.")
        }

        val settled = verdicts.count { it.settled }
        val open = verdicts.count { it.leftOpen }
        val broken = verdicts.count { it.contradictory }
        val unbuilt = verdicts.count { it.unbuilt.isNotEmpty() }
        logger.lifecycle("")
        logger.lifecycle(
            "$settled settled · $open left open · $broken contradictory · $unbuilt not measured " +
                    "(of ${verdicts.size} forks)",
        )
        if (open > 0) {
            logger.lifecycle("")
            logger.lifecycle("Leaving a choice open is not automatically wrong. But the result names")
            logger.lifecycle("a shop and a price, and someone will rely on whichever choice your code")
            logger.lifecycle("makes. Writing the decision down makes that behavior intentional.")
        }

        // -PforksStrict turns unresolved forks into a build failure.
        // Print the full report before failing.
        if (providers.gradleProperty("forksStrict").isPresent && (open > 0 || broken > 0 || unbuilt > 0)) {
            error("-PforksStrict: $open fork(s) still open, $broken contradictory, $unbuilt not checked")
        }
    }

    /**
     * True when test classes compiled but no test results were produced.
     * This helps distinguish a suite with no runnable tests from a compilation failure.
     */
    private fun ranNothing(moduleDir: File): Boolean {
        val classes = moduleDir.resolve("build/classes/kotlin/test")
        return classes.isDirectory && classes.walkTopDown().any { f -> f.extension == "class" }
    }

    private companion object {
        /**
         * Prefix for tests that should pass on every valid reading.
         * We use test names so we don't have to change the learner's test suite.
         */
        const val SETTLED_PREFIX = "settled"
    }
}
