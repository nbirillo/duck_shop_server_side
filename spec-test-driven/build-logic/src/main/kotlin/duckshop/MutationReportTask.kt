package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Reports what happened when the test suite ran against each mutant in the catalog.
 * A mutant is KILLED if a test catches the change. Otherwise, it SURVIVED.
 *
 * The catalog can check two different things:
 *
 * - `must-kill` mutants represent real defects. Good tests should catch all of them.
 *   `spec-dependent` mutants change something the contract never settled, so it's fine
 *   if they survive. We show them separately.
 * - `conformant` variants are valid refactorings. They should still pass the tests.
 *   If a test fails, that's a FALSE ALARM: the test depends on an implementation detail
 *   rather than required behavior. Exercise 11.2's advanced tier checks for this too.
 *
 * The `duck-shop.mutants` plugin connects the test tasks automatically.
 * Use `--continue` so one compilation failure doesn't hide the other results.
 */
abstract class MutationReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Catalog file, relative to the build root. */
    @get:Input
    abstract val catalogPath: Property<String>

    /** Where the generated mutant modules live, relative to the build root. */
    @get:Input
    abstract val outPath: Property<String>

    /** Use the same default test suite as the generated modules. See [GenerateMutantsTask]. */
    @get:Input
    @get:Optional
    abstract val defaultTests: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val catalogFile = catalogPath.get().trim('/')
        val outDir = outPath.get().trim('/')
        val learnerTests = pathProp("mutantsLearnerTests", "exercises/write-tests/src/test/kotlin")

        val catalog = loadCatalog(root.resolve(catalogFile))
        // Match the suite used when the modules were generated. Otherwise the report could
        // name one suite while the modules actually ran another. This happened when the
        // pricing modules used the property catalog but the report defaulted to "learner".
        val selected = providers.gradleProperty("mutantTests").getOrElse(defaultTests.getOrElse("learner"))
        val suitePath = if (selected == "learner") learnerTests else selected

        // Legal refactorings check for false alarms, so give them a different report heading.
        val conformanceOnly = catalog.all { it.conformant }

        logger.lifecycle("")
        logger.lifecycle(
            (if (conformanceOnly) "Conformance check" else "Mutation testing") + " — suite: $selected ($suitePath)",
        )

        // Check the suite against the real :core first. A test that already fails on correct
        // code doesn't prove anything about a mutant, so we exclude those failures from the score.
        val baselineResults = root.resolve("$outDir/$BASELINE/build/test-results/test")
        val baselineFailures = failingTests(baselineResults)
        val baseline = when {
            baselineFailures == null -> Status.NOT_RUN
            baselineFailures.isEmpty() -> Status.SURVIVED
            else -> Status.KILLED
        }
        val baselineWord = when (baseline) {
            Status.SURVIVED -> "VALID"
            Status.KILLED -> "INVALID"
            Status.NOT_RUN -> "NOT RUN"
        }
        // Header first, detail on its own line. The INVALID case used to run past the width of a
        // terminal and wrap badly, which is a poor look for the first line of the report.
        logger.lifecycle("Baseline (suite vs unmutated :core): $baselineWord")
        when (baseline) {
            Status.SURVIVED -> logger.lifecycle("  All tests pass on the correct code.")
            Status.KILLED -> {
                logger.lifecycle("  ${baselineFailures!!.size} test(s) already fail on the correct code.")
                logger.lifecycle("  They cannot count as mutant kills, so they are left out of the score. Fix them first.")
            }
            Status.NOT_RUN -> logger.lifecycle("  The suite did not compile against :core.")
        }

        val results = catalog.map {
            it to outcome(root.resolve("$outDir/${it.id}/build/test-results/test"), baselineFailures.orEmpty())
        }
        val width = catalog.maxOf { it.id.length } + 2

        fun report(bucket: String, title: String) {
            val group = results.filter { (mutant, _) -> mutant.bucket == bucket }
            if (group.isEmpty()) return
            logger.lifecycle("")
            logger.lifecycle(title)
            group.forEach { (mutant, outcome) ->
                val label = when {
                    // For a valid refactoring, a failing test is a false alarm, not a caught defect.
                    // It means the test is too strict about how the code is written.
                    !mutant.conformant -> outcome.status.label
                    outcome.status == Status.KILLED -> "FALSE ALARM"
                    outcome.status == Status.SURVIVED -> "OK"
                    else -> Status.NOT_RUN.label
                }
                logger.lifecycle("  ${label.padEnd(12)}${mutant.id.padEnd(width)}${mutant.what}".trimEnd())
                // The student catalog doesn't give hints. We report what survived and let
                // the learner work out which behavior needs a test.
                if (outcome.status == Status.SURVIVED && bucket == Mutant.MUST_KILL) {
                    val advice = if (mutant.hint.isEmpty()) {
                        "-> no test catches this change"
                    } else {
                        "-> try adding: ${mutant.hint}"
                    }
                    logger.lifecycle("  ${" ".padEnd(12)}${" ".padEnd(width)}$advice")
                }
                // Show which tests failed on valid code. Those are the tests to reconsider.
                if (outcome.status == Status.KILLED && mutant.conformant) {
                    val shown = outcome.newFailures.sorted().take(FALSE_ALARM_TESTS_SHOWN)
                    val more = outcome.newFailures.size - shown.size
                    logger.lifecycle(
                        "  ${" ".padEnd(12)}${" ".padEnd(width)}" +
                                "-> the behavior is unchanged, but ${outcome.newFailures.size} test(s) fail:",
                    )
                    shown.forEach { logger.lifecycle("  ${" ".padEnd(12)}${" ".padEnd(width)}     $it") }
                    if (more > 0) logger.lifecycle("  ${" ".padEnd(12)}${" ".padEnd(width)}     … and $more more")
                }
                if (outcome.status == Status.NOT_RUN) {
                    logger.lifecycle(
                        "  ${" ".padEnd(12)}${" ".padEnd(width)}" +
                                "-> the suite did not compile or run here. Try `./gradlew ${name.replace("verify", "generate")}` " +
                                "and make sure the suite uses only :core types",
                    )
                }
            }
        }

        report(Mutant.MUST_KILL, "Must-kill mutants — your tests should catch all of these:")
        report(
            Mutant.SPEC_DEPENDENT,
            "Spec-dependent mutants — these may survive (see mutants/README.md):",
        )
        report(
            Mutant.CONFORMANT,
            "Conformant variants — behavior is unchanged, so all tests should pass:",
        )

        val mustKill = results.filter { (mutant, _) -> mutant.mustKill }
        val killed = mustKill.count { (_, outcome) -> outcome.status == Status.KILLED }
        val notRun = results.count { (_, outcome) -> outcome.status == Status.NOT_RUN }
        val conformant = results.filter { (mutant, _) -> mutant.conformant }
        val falseAlarms = conformant.count { (_, outcome) -> outcome.status == Status.KILLED }

        logger.lifecycle("")
        if (mustKill.isNotEmpty()) {
            val percent = killed * 100 / mustKill.size
            logger.lifecycle("Mutation score: $killed/${mustKill.size} must-kill mutants killed ($percent%).")
        }
        if (conformant.isNotEmpty()) {
            val accepted = conformant.size - falseAlarms -
                    conformant.count { (_, outcome) -> outcome.status == Status.NOT_RUN }
            logger.lifecycle(
                "Conformance: $accepted/${conformant.size} valid refactorings accepted" +
                        if (falseAlarms == 0) "." else ", $falseAlarms false alarm(s).",
            )
        }
        // The advanced tier of 11.2 limits the number of tests. Without a limit, it's
        // easy to add a test for every case. A budget makes you choose the most useful ones.
        val tests = countTests(baselineResults)
        val budget = providers.gradleProperty("testBudget").orNull?.toIntOrNull()
        val overBudget = budget != null && tests != null && tests > budget
        if (tests != null) {
            logger.lifecycle(
                "Suite size: $tests test(s)" + when {
                    budget == null -> "."
                    overBudget -> " — above the limit of $budget by ${tests - budget}."
                    else -> " — within the limit of $budget."
                },
            )
        }
        if (baseline != Status.SURVIVED) {
            logger.lifecycle("The tests do not pass on the original code, so the scores above are not reliable.")
        }
        if (notRun > 0) logger.lifecycle("$notRun entries did not run — the results above are incomplete.")
        if (overBudget) {
            logger.lifecycle(
                "Reduce the suite to $budget tests. Look for tests checking the same thing more than " +
                        "once, and keep the most useful one.",
            )
        }

        val clean = killed == mustKill.size && falseAlarms == 0 && notRun == 0 &&
                baseline == Status.SURVIVED && !overBudget
        when {
            clean && conformanceOnly ->
                logger.lifecycle("No tests depend on implementation details the contract leaves open. ✅")
            clean ->
                logger.lifecycle("All must-kill mutants were caught. ✅")
            falseAlarms > 0 -> logger.lifecycle(
                "Review the tests listed above. They fail even though the code behaves like :core, so they " +
                        "restrict how the code is written, not what it does.",
            )
            else -> logger.lifecycle(
                "Add stronger tests: each surviving mutant shows a behavior your tests do not check.",
            )
        }

        // If a test budget was provided, going over it fails the task even without
        // -PmutantsStrict. Print the full report first so the reason is clear.
        if (overBudget) {
            error(
                "Suite size $tests exceeds -PtestBudget=$budget. The test limit is part of the task: a suite " +
                        "that checks the contract with fewer tests is better.",
            )
        }

        if (providers.gradleProperty("mutantsStrict").isPresent && !clean) {
            error(
                "Mutation score $killed/${mustKill.size}, $falseAlarms false alarm(s), baseline $baselineWord — " +
                        "-PmutantsStrict needs tests that pass on correct code, catch every must-kill mutant, and raise no " +
                        "false alarms.",
            )
        }
    }

    private fun pathProp(name: String, default: String): String =
        providers.gradleProperty(name).getOrElse(default).trim('/')

    private enum class Status(val label: String) {
        KILLED("KILLED"),
        SURVIVED("SURVIVED"),
        NOT_RUN("NOT RUN"),
    }

    /** Result for one mutant, including tests that failed only on that mutant. */
    private data class Outcome(val status: Status, val newFailures: Set<String>)

    /**
     * A mutant is killed only when a test fails on the mutant but passes on the
     * original code. A test that fails on both doesn't count.
     */
    private fun outcome(resultsDir: File, baselineFailures: Set<String>): Outcome {
        val failing = failingTests(resultsDir) ?: return Outcome(Status.NOT_RUN, emptySet())
        val new = failing - baselineFailures
        return Outcome(if (new.isNotEmpty()) Status.KILLED else Status.SURVIVED, new)
    }

    /** Count tests in the baseline results, or return null if there are no results. */
    private fun countTests(resultsDir: File): Int? {
        val xmls = resultsDir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" }
            ?: return null
        if (xmls.isEmpty()) return null
        return xmls.sumOf { xml -> xml.readText().split("<testcase ").size - 1 }
    }

    /** Find tests that failed or errored, or return null if there are no results. */
    private fun failingTests(resultsDir: File): Set<String>? {
        val xmls = resultsDir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" }
            ?: return null
        if (xmls.isEmpty()) return null
        return xmls.flatMap { xml ->
            // Split the JUnit XML into test cases. If a case contains <failure or <error,
            // count it as failed.
            xml.readText().split("<testcase ").drop(1).mapNotNull { chunk ->
                val name = Regex("name=\"([^\"]*)\"").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
                val body = chunk.substringBefore("</testcase>")
                // Kotlin test names can end in (). Remove that suffix to keep the report readable.
                if (body.contains("<failure") || body.contains("<error")) name.removeSuffix("()") else null
            }
        }.toSet()
    }

    private companion object {
        /** Show a few failing test names without flooding the report. */
        const val FALSE_ALARM_TESTS_SHOWN = 5
    }
}

