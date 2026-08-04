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
 * Scores one attacking implementation (`runAgent -Pmode=attack`) against the suite it was aimed at.
 *
 * A mutant catalog is a fixed list, so a suite can eventually kill all of it and stop learning. An
 * adversary cannot be saturated: every time the suite improves, the next attack has to find a gap
 * further out. What makes that automatic is the oracle — the attack is compared with `:core` by
 * running the same [DifferentialProbe] in both modules and diffing the recorded answers, so no
 * hidden answer key and no teacher is needed to decide whether an implementation is really wrong.
 *
 * Three outcomes:
 *  - the suite fails against the attack — it caught it, and the attack failed;
 *  - the suite passes and the probe finds no disagreement — the "attack" is a correct implementation
 *    written differently, which is also a failed attack;
 *  - the suite passes and the probe disagrees — the suite has a hole, and the first disagreeing case
 *    is the counterexample to write a test for.
 */
abstract class AttackReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Folder name under `attacks/` to score. */
    @get:Input
    abstract val agent: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val name = agent.orNull ?: error("Missing -Pagent=<name> (a folder under attacks/)")
        val attackDir = root.resolve("attacks/$name")
        require(attackDir.isDirectory) {
            "attacks/$name does not exist — generate it first with " +
                "./gradlew runAgent -Pmode=attack -Pprovider=<p> -Pmodel=<m>"
        }

        logger.lifecycle("")
        logger.lifecycle("Attack — agent: $name")

        val referenceDir = root.resolve("attacks/reference")
        val rawFailures = failingTests(attackDir.resolve("build/test-results/test"))
        // A test that is already red against the correct algebra fails against anything, so counting
        // it as the suite catching the attack would turn a broken test into a false victory. This is
        // the same correction the mutation report applies.
        val alreadyRed = failingTests(referenceDir.resolve("build/test-results/test")).orEmpty()
        val suiteFailures = rawFailures?.minus(alreadyRed)
        val attackProbe = readProbe(attackDir)
        val referenceProbe = readProbe(referenceDir)

        if (alreadyRed.isNotEmpty()) {
            logger.lifecycle(
                "${alreadyRed.size} test(s) in this suite fail against the correct algebra, so they say " +
                    "nothing about an attack and are excluded. Fix them first.",
            )
        }

        if (rawFailures == null) {
            logger.lifecycle(
                "The attacking implementation did not compile or did not run. That is not a verdict on " +
                    "your suite — inspect attacks/$name/src and re-run the model if the output contract " +
                    "was broken.",
            )
            return
        }
        if (attackProbe == null || referenceProbe == null) {
            logger.lifecycle(
                "No probe output. Run `./gradlew :attacks:reference:test :attacks:$name:test` first " +
                    "(verifyAttack normally does that for you).",
            )
            return
        }

        val caught = suiteFailures!!.isNotEmpty()
        logger.lifecycle(
            "Your suite against the attacking implementation: " + if (caught) {
                "FAILED — ${suiteFailures.size} test(s) caught it"
            } else {
                "PASSED — no test noticed anything"
            },
        )

        val disagreements = referenceProbe.zip(attackProbe).filter { (reference, attack) -> reference != attack }
        logger.lifecycle(
            "Behaviour against :core over ${referenceProbe.size} probed cases: " + if (disagreements.isEmpty()) {
                "IDENTICAL"
            } else {
                "DIFFERS in ${disagreements.size}"
            },
        )

        if (disagreements.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle("First disagreement:")
            val (reference, attack) = disagreements.first()
            logger.lifecycle("  case:   ${reference.substringBefore(" -> ").substringAfter('\t')}")
            logger.lifecycle("  :core   says ${reference.substringAfterLast(" -> ")}")
            logger.lifecycle("  attack  says ${attack.substringAfterLast(" -> ")}")
        }

        logger.lifecycle("")
        val succeeded = !caught && disagreements.isNotEmpty()
        when {
            caught -> logger.lifecycle(
                "Your suite rejected the attack. ✅ It pins down the behaviour this implementation tried " +
                    "to change — try a stronger attacker, or attack again now that the suite has grown.",
            )
            disagreements.isEmpty() -> logger.lifecycle(
                "The attack failed on its own terms: it answers exactly like :core for every probed case, " +
                    "so it is a correct implementation written differently, not a counterexample. Nothing " +
                    "to conclude about your suite from this run.",
            )
            else -> logger.lifecycle(
                "The attack succeeded. ❌ An implementation your suite accepts answers differently from " +
                    ":core. Add a test that pins the case above, then attack again — the point is not to " +
                    "win once but to keep going until the attacker runs out of room.",
            )
        }

        if (succeeded && providers.gradleProperty("mutantsStrict").isPresent) {
            error("The attack succeeded — -PmutantsStrict requires a suite no attacking implementation gets past.")
        }
    }

    /** The probe's recorded answers, in order, or null if the probe did not run. */
    private fun readProbe(moduleDir: File): List<String>? =
        moduleDir.resolve("build/probe.txt").takeIf { it.isFile }?.readLines()?.filter { it.isNotBlank() }

    /**
     * Names of the suite's failing tests, or null if nothing ran at all. The probe is not part of the
     * suite, so its results never count as the suite catching anything.
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
