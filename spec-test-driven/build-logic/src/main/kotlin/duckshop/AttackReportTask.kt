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
 * Checks one agent-written attack (`runAgent -Pmode=attack`) against the tests it tried to beat.
 *
 * A fixed mutant catalog can eventually be fully covered. An attacker is different: whenever
 * the tests improve, it can try to find another gap.
 *
 * We check whether the attack is actually wrong by running the same [DifferentialProbe] against
 * the attack and the correct `:core` implementation, then comparing their answers. This doesn't
 * need a hidden answer key or a teacher to judge each attempt.
 *
 * There are three possible results:
 * - The tests fail on the attack: the tests caught it, so the attack failed.
 * - The tests pass, and the probe finds no differences: the attack behaves like correct code.
 *   That's also a failed attack.
 * - The tests pass, but the probe finds a difference: the attack found a gap in the tests.
 *   The first difference gives you a case to test next.
 */
abstract class AttackReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Name of the attack folder under `attacks/`. */
    @get:Input
    abstract val agent: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val name = agent.orNull ?: error("Missing -Pagent=<name> (choose a folder under attacks/)")
        val attackDir = root.resolve("attacks/$name")
        require(attackDir.isDirectory) {
            "attacks/$name does not exist. Generate an attack first with " +
                    "./gradlew runAgent -Pmode=attack -Pprovider=<p> -Pmodel=<m>"
        }

        logger.lifecycle("")
        logger.lifecycle("Attack check — agent: $name")

        val referenceDir = root.resolve("attacks/reference")
        val rawFailures = failingTests(attackDir.resolve("build/test-results/test"))
        // A test that already fails on correct code doesn't prove the attack is wrong.
        // Leave those failures out, just as we do in the mutation report.
        val alreadyRed = failingTests(referenceDir.resolve("build/test-results/test")).orEmpty()
        val suiteFailures = rawFailures?.minus(alreadyRed)
        val attackProbe = readProbe(attackDir)
        val referenceProbe = readProbe(referenceDir)

        if (alreadyRed.isNotEmpty()) {
            logger.lifecycle(
                "${alreadyRed.size} test(s) already fail on correct code. They cannot count as catching " +
                        "the attack, so they are excluded. Fix them first.",
            )
        }

        if (rawFailures == null) {
            logger.lifecycle(
                "The attack did not compile or run, so we can't judge your tests yet. " +
                        "Check attacks/$name/src and try running the model again if it returned broken code.",
            )
            return
        }
        if (attackProbe == null || referenceProbe == null) {
            logger.lifecycle(
                "No probe results found. Run `./gradlew :attacks:reference:test :attacks:$name:test` first " +
                        "(verifyAttack normally runs these for you).",
            )
            return
        }

        val caught = suiteFailures!!.isNotEmpty()
        logger.lifecycle(
            "Your tests against the attack: " + if (caught) {
                "FAILED — ${suiteFailures.size} test(s) caught the attack"
            } else {
                "PASSED — no test caught the attack"
            },
        )

        val disagreements = referenceProbe.zip(attackProbe).filter { (reference, attack) -> reference != attack }
        logger.lifecycle(
            "Results compared with :core across ${referenceProbe.size} probe cases: " + if (disagreements.isEmpty()) {
                "IDENTICAL"
            } else {
                "DIFFERS in ${disagreements.size}"
            },
        )

        if (disagreements.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("First difference:")
            val (reference, attack) = disagreements.first()
            logger.lifecycle("  case:   ${reference.substringBefore(" -> ").substringAfter('\t')}")
            logger.lifecycle("  :core   says ${reference.substringAfterLast(" -> ")}")
            logger.lifecycle("  attack  says ${attack.substringAfterLast(" -> ")}")
        }

        logger.lifecycle("")
        val succeeded = !caught && disagreements.isNotEmpty()
        when {
            caught -> logger.lifecycle(
                "Your tests caught the attack. ✅ They check the behavior the attack tried to change. " +
                        "Try a stronger attacker, or run another attack after improving your tests.",
            )
            disagreements.isEmpty() -> logger.lifecycle(
                "The attack failed: it gave the same answers as :core for every checked case. " +
                        "It may look different, but the probe found no incorrect behavior. " +
                        "This run doesn't tell us whether your tests are strong enough.",
            )
            else -> logger.lifecycle(
                "The attack succeeded. ❌ Your tests passed an implementation that disagrees with :core. " +
                        "Add a test for the case above, then try another attack. " +
                        "The goal is to keep closing gaps, not just catch one attack.",
            )
        }

        if (succeeded && providers.gradleProperty("mutantsStrict").isPresent) {
            error("The attack got past your tests. -PmutantsStrict requires a suite that catches every successful attack.")
        }
    }

    /** Read the probe answers in order, or return null if there are no results. */
    private fun readProbe(moduleDir: File): List<String>? =
        moduleDir.resolve("build/probe.txt").takeIf { it.isFile }?.readLines()?.filter { it.isNotBlank() }

    /**
     * Find the tests that failed, or return null if the suite didn't run.
     * Probe results don't count as tests catching an attack.
     */
    private fun failingTests(resultsDir: File): Set<String>? {
        val xmls = resultsDir.listFiles { f ->
            f.name.startsWith("TEST-") && f.extension == "xml" && !f.name.contains("DifferentialProbe")
        } ?: return null
        if (xmls.isEmpty()) return null
        return xmls.flatMap { xml ->
            xml.readText().split("<testcase ").drop(1).mapNotNull { chunk ->
                val name = Regex("name=\"([^\"]*)\"").find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
                val body = chunk.substringBefore("</testcase>")
                if (body.contains("<failure") || body.contains("<error")) name.removeSuffix("()") else null
            }
        }.toSet()
    }
}
