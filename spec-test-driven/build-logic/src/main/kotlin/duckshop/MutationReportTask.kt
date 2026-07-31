package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Mutation testing report: for every mutant in the catalog, says whether the suite that ran against
 * it KILLED it (some test failed there) or it SURVIVED, and prints the mutation score.
 *
 * Only `must-kill` mutants count towards the score. `spec-dependent` mutants are listed separately:
 * they change behaviour the specification never promised, so their survival is acceptable — see
 * `mutants/README.md`.
 *
 * The test tasks themselves are wired as dependencies by the `duck-shop.mutants` plugin. Use
 * `--continue` so one suite that fails to compile against a mutant does not hide the other results.
 */
abstract class MutationReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val catalogPath = pathProp("mutantsCatalog", "mutants/catalog.json")
        val outPath = pathProp("mutantsOut", "mutants")
        val learnerTests = pathProp("mutantsLearnerTests", "exercises/write-tests/src/test/kotlin")
        val authoredTests = pathProp("mutantsAuthoredTests", "tests/kotlin")

        val catalog = loadCatalog(root.resolve(catalogPath))
        val selected = providers.gradleProperty("mutantTests").getOrElse("learner")
        val suitePath = when (selected) {
            "learner" -> learnerTests
            "authored" -> authoredTests
            else -> selected
        }

        logger.lifecycle("")
        logger.lifecycle("Mutation testing — suite: $selected ($suitePath)")

        // A suite that is red on the unmutated :core would "kill" every mutant with that same
        // failure, so validity comes first — this mirrors step 1 of the exercise. Tests that already
        // fail on the correct code are excluded below, so the score stays honest either way.
        val baselineFailures = failingTests(root.resolve("$outPath/$BASELINE/build/test-results/test"))
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
            it to status(root.resolve("$outPath/${it.id}/build/test-results/test"), baselineFailures.orEmpty())
        }
        val width = catalog.maxOf { it.id.length } + 2

        fun report(bucket: String, title: String) {
            val group = results.filter { (mutant, _) -> mutant.bucket == bucket }
            if (group.isEmpty()) return
            logger.lifecycle("")
            logger.lifecycle(title)
            group.forEach { (mutant, status) ->
                logger.lifecycle("  ${status.label.padEnd(10)}${mutant.id.padEnd(width)}${mutant.what}")
                if (status == Status.SURVIVED && bucket == Mutant.MUST_KILL) {
                    logger.lifecycle("  ${" ".padEnd(10)}${" ".padEnd(width)}-> add: ${mutant.hint}")
                }
                if (status == Status.NOT_RUN) {
                    logger.lifecycle(
                        "  ${" ".padEnd(10)}${" ".padEnd(width)}" +
                            "-> the suite did not compile/run here; run `./gradlew generateMutants` " +
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

        val mustKill = results.filter { (mutant, _) -> mutant.mustKill }
        val killed = mustKill.count { (_, status) -> status == Status.KILLED }
        val notRun = mustKill.count { (_, status) -> status == Status.NOT_RUN }
        val percent = if (mustKill.isEmpty()) 100 else killed * 100 / mustKill.size

        logger.lifecycle("")
        logger.lifecycle("Mutation score: $killed/${mustKill.size} must-kill mutants killed ($percent%).")
        if (baseline != Status.SURVIVED) {
            logger.lifecycle("The baseline is not green, so this score cannot be trusted — see above.")
        }
        if (notRun > 0) logger.lifecycle("$notRun mutant(s) never ran — the score above is incomplete.")
        if (killed == mustKill.size && notRun == 0 && baseline == Status.SURVIVED) {
            logger.lifecycle("Every must-kill mutant is dead. ✅")
        } else {
            logger.lifecycle("Strengthen the suite: each surviving mutant is a behaviour no test pins down.")
        }

        if (providers.gradleProperty("mutantsStrict").isPresent &&
            (killed != mustKill.size || notRun > 0 || baseline != Status.SURVIVED)
        ) {
            error(
                "Mutation score $killed/${mustKill.size}, baseline $baselineWord — -PmutantsStrict " +
                    "requires a valid suite that kills every must-kill mutant.",
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

    /**
     * A mutant is killed when the suite fails a test there that it does NOT already fail on the
     * unmutated code — a test that is red either way says nothing about the mutant.
     */
    private fun status(resultsDir: File, baselineFailures: Set<String>): Status {
        val failing = failingTests(resultsDir) ?: return Status.NOT_RUN
        return if ((failing - baselineFailures).isNotEmpty()) Status.KILLED else Status.SURVIVED
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
                if (body.contains("<failure") || body.contains("<error")) name else null
            }
        }.toSet()
    }
}
