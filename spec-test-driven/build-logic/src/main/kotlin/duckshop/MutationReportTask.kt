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
 * Mutation testing report: for every entry in the catalog, says whether the suite that ran against
 * it KILLED it (some test failed there) or it SURVIVED, and prints the score.
 *
 * The catalog decides what the report measures, so the same task serves both directions:
 *
 * - `must-kill` mutants are defects; a faithful suite kills every one of them, and the headline is
 *   the mutation score. `spec-dependent` mutants change behaviour the specification never promised,
 *   so their survival is acceptable and they are listed separately.
 * - `conformant` entries are the opposite: legal refactorings that leave behaviour unchanged. The
 *   suite must stay GREEN on them, and a failure is a FALSE ALARM — a test pinning an implementation
 *   detail the contract leaves free. That direction is what the advanced tier of exercise 11.2 adds,
 *   because a suite can reach a perfect mutation score by over-specifying.
 *
 * The test tasks themselves are wired as dependencies by the `duck-shop.mutants` plugin. Use
 * `--continue` so one suite that fails to compile against a mutant does not hide the other results.
 */
abstract class MutationReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Catalog to report on, relative to this build's root. */
    @get:Input
    abstract val catalogPath: Property<String>

    /** Directory holding the generated modules for that catalog, relative to this build's root. */
    @get:Input
    abstract val outPath: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val catalogFile = catalogPath.get().trim('/')
        val outDir = outPath.get().trim('/')
        val learnerTests = pathProp("mutantsLearnerTests", "exercises/write-tests/src/test/kotlin")

        val catalog = loadCatalog(root.resolve(catalogFile))
        val selected = providers.gradleProperty("mutantTests").getOrElse("learner")
        val suitePath = if (selected == "learner") learnerTests else selected

        // A catalog of legal refactorings measures the opposite property, so it gets its own wording.
        val conformanceOnly = catalog.all { it.conformant }

        logger.lifecycle("")
        logger.lifecycle(
            (if (conformanceOnly) "Conformance check" else "Mutation testing") + " — suite: $selected ($suitePath)",
        )

        // A suite that is red on the unmutated :core would "kill" every mutant with that same
        // failure, so validity comes first — this mirrors step 1 of the exercise. Tests that already
        // fail on the correct code are excluded below, so the score stays honest either way.
        val baselineFailures = failingTests(root.resolve("$outDir/$BASELINE/build/test-results/test"))
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
        logger.lifecycle(
            "Baseline (suite vs unmutated :core): $baselineWord — " + when (baseline) {
                Status.SURVIVED -> "no test contradicts the correct code"
                Status.KILLED -> "${baselineFailures!!.size} test(s) fail on the CORRECT code, so they " +
                    "prove nothing about a mutant; fix them first. They are excluded from the score below"
                Status.NOT_RUN -> "the suite did not compile against :core"
            },
        )

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
                    // For a legal refactoring the verdict reads the other way round: a suite that
                    // fails here has not found a defect, it has over-specified the contract.
                    !mutant.conformant -> outcome.status.label
                    outcome.status == Status.KILLED -> "FALSE ALARM"
                    outcome.status == Status.SURVIVED -> "OK"
                    else -> Status.NOT_RUN.label
                }
                logger.lifecycle("  ${label.padEnd(12)}${mutant.id.padEnd(width)}${mutant.what}".trimEnd())
                // The learner-visible catalog carries no prose: a survivor is reported as a fact,
                // and working out which test kills it is the exercise.
                if (outcome.status == Status.SURVIVED && bucket == Mutant.MUST_KILL) {
                    val advice = if (mutant.hint.isEmpty()) {
                        "-> nothing in this suite notices that change"
                    } else {
                        "-> add: ${mutant.hint}"
                    }
                    logger.lifecycle("  ${" ".padEnd(12)}${" ".padEnd(width)}$advice")
                }
                // Naming the tests is the whole diagnostic here: they are the ones to relax.
                if (outcome.status == Status.KILLED && mutant.conformant) {
                    val shown = outcome.newFailures.sorted().take(FALSE_ALARM_TESTS_SHOWN)
                    val more = outcome.newFailures.size - shown.size
                    logger.lifecycle(
                        "  ${" ".padEnd(12)}${" ".padEnd(width)}" +
                            "-> behaviour is unchanged, yet ${outcome.newFailures.size} test(s) fail:",
                    )
                    shown.forEach { logger.lifecycle("  ${" ".padEnd(12)}${" ".padEnd(width)}     $it") }
                    if (more > 0) logger.lifecycle("  ${" ".padEnd(12)}${" ".padEnd(width)}     … and $more more")
                }
                if (outcome.status == Status.NOT_RUN) {
                    logger.lifecycle(
                        "  ${" ".padEnd(12)}${" ".padEnd(width)}" +
                            "-> the suite did not compile/run here; run `./gradlew ${name.replace("verify", "generate")}` " +
                            "and check the suite only uses :core types",
                    )
                }
            }
        }

        report(Mutant.MUST_KILL, "Must-kill mutants — a faithful suite kills every one of these:")
        report(
            Mutant.SPEC_DEPENDENT,
            "Spec-dependent mutants — survival is acceptable (see mutants/README.md):",
        )
        report(
            Mutant.CONFORMANT,
            "Conformant variants — the behaviour is unchanged, so a faithful suite stays GREEN on all of these:",
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
                "Conformance: $accepted/${conformant.size} legal refactorings accepted" +
                    if (falseAlarms == 0) "." else ", $falseAlarms false alarm(s).",
            )
        }
        if (baseline != Status.SURVIVED) {
            logger.lifecycle("The baseline is not green, so the numbers above cannot be trusted — see above.")
        }
        if (notRun > 0) logger.lifecycle("$notRun entr(y/ies) never ran — the numbers above are incomplete.")

        val clean = killed == mustKill.size && falseAlarms == 0 && notRun == 0 && baseline == Status.SURVIVED
        when {
            clean && conformanceOnly ->
                logger.lifecycle("No test pins a detail the contract leaves free. ✅")
            clean ->
                logger.lifecycle("Every must-kill mutant is dead. ✅")
            falseAlarms > 0 -> logger.lifecycle(
                "Relax the tests listed above: they fail on code that behaves exactly like :core, so they " +
                    "constrain how the algebra is written rather than what it decides.",
            )
            else -> logger.lifecycle(
                "Strengthen the suite: each surviving mutant is a behaviour no test pins down.",
            )
        }

        if (providers.gradleProperty("mutantsStrict").isPresent && !clean) {
            error(
                "Mutation score $killed/${mustKill.size}, $falseAlarms false alarm(s), baseline $baselineWord — " +
                    "-PmutantsStrict requires a valid suite that kills every must-kill mutant and raises no " +
                    "false alarm.",
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

    /** How one suite fared against one generated module, and which tests newly failed there. */
    private data class Outcome(val status: Status, val newFailures: Set<String>)

    /**
     * A mutant is killed when the suite fails a test there that it does NOT already fail on the
     * unmutated code — a test that is red either way says nothing about the mutant.
     */
    private fun outcome(resultsDir: File, baselineFailures: Set<String>): Outcome {
        val failing = failingTests(resultsDir) ?: return Outcome(Status.NOT_RUN, emptySet())
        val new = failing - baselineFailures
        return Outcome(if (new.isNotEmpty()) Status.KILLED else Status.SURVIVED, new)
    }

    /** Names of the tests that failed or errored in a run, or null if the run produced no results. */
    private fun failingTests(resultsDir: File): Set<String>? {
        val xmls = resultsDir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" }
            ?: return null
        if (xmls.isEmpty()) return null
        return xmls.flatMap { xml ->
            // One chunk per <testcase …>; a chunk that mentions <failure/<error before its closing
            // tag is a failing test. Good enough for the JUnit XML the test task writes.
            xml.readText().split("<testcase ").drop(1).mapNotNull { chunk ->
                val name = Regex("name=\"([^\"]*)\"").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
                val body = chunk.substringBefore("</testcase>")
                // Kotlin test methods arrive as `a backticked name()`; the parentheses only add noise
                // when the report prints the tests to relax.
                if (body.contains("<failure") || body.contains("<error")) name.removeSuffix("()") else null
            }
        }.toSet()
    }

    private companion object {
        /** Enough to see the pattern without burying the report when a suite over-specifies broadly. */
        const val FALSE_ALARM_TESTS_SHOWN = 5
    }
}
