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
 * **How much did the specification leave to chance?**
 *
 * Two independent implementations of the *same* text, compared with each other and with the
 * reference. This exists because scoring one implementation against the reference stops
 * discriminating as the implementer gets stronger: we measured a specification with no rounding
 * rule, no compounding rule and a self-contradictory bonus coming out 8/8, level with the best in
 * the corpus, because a capable agent fills every gap with the same default the reference chose.
 *
 * Agreement is the measurement, so no model has to be believed about anything:
 *
 *  - **settled, as we did** — A and B agree, and agree with the reference.
 *  - **settled differently** — A and B agree with each other and not with the reference. This is
 *    *not* a defect. Two readers reached the same answer from the text; ours is one legal answer
 *    among others, and the specification is doing its job.
 *  - **left open** — A and B differ. The text did not decide, and each agent decided for itself.
 *    This is the number the reference-agreement score cannot see.
 *
 * A specification is not better for having fewer open cases in every situation — deliberately
 * leaving something free is a legitimate choice, and section 4 of the template exists to record it.
 * What this measures is how much is open *at all*; whether that was on purpose is in the text.
 *
 * Params: `-PagentA=<run>`, `-PagentB=<run>`, `[-Pspec=<one specification>]`, `[-Pexamples=<n>]`.
 */
abstract class DivergenceReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val referenceRecording: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val a = providers.gradleProperty("agentA").orNull ?: error("Missing -PagentA=<run>")
        val b = providers.gradleProperty("agentB").orNull ?: error("Missing -PagentB=<run>")
        require(a != b) { "-PagentA and -PagentB are the same run ($a); nothing to compare." }
        val examples = providers.gradleProperty("examples").orNull?.toInt() ?: 3

        val reference = root.resolve(referenceRecording.get())
            .takeIf { it.isFile }
            ?.readLines()
            ?: run {
                logger.lifecycle("No reference recording — run :reference-probe:test first.")
                null
            }

        val only = providers.gradleProperty("spec").orNull?.let(::keyOf)
        val specs = (recordings(root, a).keys + recordings(root, b).keys)
            .filter { only == null || it == only }
            .sorted()
        require(specs.isNotEmpty()) { "No recordings for $a / $b. Run their :test tasks first." }

        logger.lifecycle("")
        logger.lifecycle("How much each specification left to chance — $a vs $b")
        logger.lifecycle("Two independent implementations of the same text. No model is trusted here;")
        logger.lifecycle("the measurement is whether they agree.")

        val ra = recordings(root, a)
        val rb = recordings(root, b)
        specs.forEach { spec ->
            val la = ra[spec]?.readLines()
            val lb = rb[spec]?.readLines()
            logger.lifecycle("")
            if (la == null || lb == null) {
                logger.lifecycle("── $spec — only one run recorded (${if (la == null) b else a})")
                return@forEach
            }
            report(spec, la, lb, reference, examples)
        }
    }

    private fun report(spec: String, la: List<String>, lb: List<String>, ref: List<String>?, examples: Int) {
        require(la.size == lb.size) {
            "$spec: recordings are ${la.size} and ${lb.size} lines. The probe corpus changed between " +
                "runs, so the two are not comparable — re-run both."
        }
        var open = 0
        var differentlySettled = 0
        val openExamples = mutableListOf<String>()
        val settledExamples = mutableListOf<String>()

        la.indices.forEach { i ->
            val inputA = la[i].substringBefore(" => ")
            val outA = la[i].substringAfter(" => ")
            val outB = lb[i].substringAfter(" => ")
            when {
                outA != outB -> {
                    open++
                    if (openExamples.size < examples) openExamples += "$inputA => $outA / $outB"
                }
                ref != null && outA != ref[i].substringAfter(" => ") -> {
                    differentlySettled++
                    if (settledExamples.size < examples) {
                        settledExamples += "$inputA => both $outA, reference ${ref[i].substringAfter(" => ")}"
                    }
                }
            }
        }

        val total = la.size
        val settled = total - open - differentlySettled
        logger.lifecycle("── $spec")
        logger.lifecycle("   $total probed inputs")
        logger.lifecycle("   settled, same as the reference   $settled")
        logger.lifecycle("   settled differently              $differentlySettled")
        logger.lifecycle("   LEFT OPEN                        $open")
        settledExamples.forEach { logger.lifecycle("     settled differently: $it") }
        if (settledExamples.isNotEmpty()) {
            logger.lifecycle("       ↳ both readers agreed and neither matched us. The text decided this;")
            logger.lifecycle("         our reference is one legal answer, not the only one.")
        }
        openExamples.forEach { logger.lifecycle("     open: $it") }
        if (openExamples.isNotEmpty()) {
            logger.lifecycle("       ↳ the text did not decide, and each agent decided for itself. Whether")
            logger.lifecycle("         that was on purpose is in your section 4, not in this report.")
        }
    }

    private fun recordings(root: File, agent: String): Map<String, File> =
        (root.resolve("implementations/$agent").listFiles() ?: emptyArray())
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                dir.resolve("build/probe/recording.txt").takeIf { it.isFile }?.let { dir.name to it }
            }
            .toMap()

    private fun keyOf(spec: String) =
        spec.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
            .takeLast(2).joinToString("-")
}
