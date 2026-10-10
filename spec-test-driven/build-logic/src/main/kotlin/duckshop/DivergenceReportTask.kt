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
 * Checks how much two agents had to guess when implementing the same specification.
 * It compares their recorded answers, and also checks against the reference when available.
 * No AI model judges the results.
 *
 * Why compare two agents? A strong agent can fill in missing rules and still match the
 * reference. We saw a vague specification score 8/8 even though it never settled
 * rounding, compounding, or a contradictory bonus.
 *
 * The report separates three outcomes:
 * - Both agree with the reference: they made the same choices as our implementation.
 * - Both agree with each other, but not the reference: they may have followed a clear
 *   alternative rule, or they may simply share the same assumption. Read the spec to tell.
 * - LEFT OPEN: the agents disagree, so the text allowed different interpretations.
 *
 * Not every open choice is a problem. A spec can deliberately leave behavior flexible.
 * The report counts disagreements; the spec tells you whether they were intentional.
 *
 * Use TWO DIFFERENT agents that can both handle the task. In calibration, two runs of
 * one model found 0 open cases out of 4021, while different agents found 674.
 * Both runs of the same model can make the same guess, even with different code.
 *
 * The second agent must also be capable. A weaker reader once reported 391 open cases
 * on a compressed spec; a stronger reader found 0 out of 4021. Those differences came
 * from the weaker model's errors, not from missing requirements.
 *
 * Agreement alone does not prove a specification is complete. Disagreement is useful
 * evidence of different interpretations, but check that neither model simply made a bug.
 * This task cannot tell which models produced the runs, so it reminds you in the report.
 *
 * Options: `-PagentA=<run>`, `-PagentB=<run>`, `[-Pspec=<one specification>]`,
 * `[-Pexamples=<n>]`.
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
        val a = providers.gradleProperty("agentA").orNull ?: error("Please provide -PagentA=<run>")
        val b = providers.gradleProperty("agentB").orNull ?: error("Please provide -PagentB=<run>")
        require(a != b) { "-PagentA and -PagentB both point to $a. Choose two different runs." }
        val examples = providers.gradleProperty("examples").orNull?.toInt() ?: 3

        val reference = root.resolve(referenceRecording.get())
            .takeIf { it.isFile }
            ?.readLines()
            ?: run {
                // The student project has no reference, and that's fine. We can still compare
                // the two agents with each other. Only the teacher build has a reference.
                logger.lifecycle("")
                logger.lifecycle("No reference available. We can still compare where the two agents agree or disagree.")
                null
            }

        val only = providers.gradleProperty("spec").orNull?.let(::keyOf)
        val specs = (recordings(root, a).keys + recordings(root, b).keys)
            .filter { only == null || it == only }
            .sorted()
        require(specs.isNotEmpty()) { "No recordings found for $a / $b. Run both :test tasks first." }

        logger.lifecycle("")
        logger.lifecycle("How much did the spec leave open? Comparing $a and $b")
        logger.lifecycle("Two implementations from the same specification. We do not ask a model to judge them;")
        logger.lifecycle("we compare their recorded answers instead.")
        // Always show this reminder. Run names don't tell us whether two runs used
        // the same model; guessing from names caused misleading warnings before.
        logger.lifecycle("")
        logger.lifecycle("Choose your agents carefully. Two runs of the SAME model often make the same guesses, so")
        logger.lifecycle("agreement is not proof the spec is complete (we measured 0 open cases where two")
        logger.lifecycle("different agents found 674). Disagreement may reveal an open choice. A model too weak")
        logger.lifecycle("for the task can also make mistakes that look like gaps in the spec.")

        val ra = recordings(root, a)
        val rb = recordings(root, b)
        specs.forEach { spec ->
            val la = ra[spec]?.readLines()
            val lb = rb[spec]?.readLines()
            logger.lifecycle("")
            if (la == null || lb == null) {
                logger.lifecycle("── $spec — only one recording found (${if (la == null) b else a})")
                return@forEach
            }
            report(spec, la, lb, reference, examples)
        }
    }

    private fun report(spec: String, la: List<String>, lb: List<String>, ref: List<String>?, examples: Int) {
        require(la.size == lb.size) {
            "$spec: recordings have ${la.size} and ${lb.size} lines. The probe cases changed between " +
                    "runs, so we cannot compare them. Run both again."
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
        logger.lifecycle("   $total cases checked")
        if (ref == null) {
            // Without a reference, report agreement between the agents only.
            logger.lifecycle("   agents agreed on                $settled")
            logger.lifecycle("   LEFT OPEN                        $open")
        } else {
            logger.lifecycle("   both match the reference        $settled")
            logger.lifecycle("   both agree, but not with reference $differentlySettled")
            logger.lifecycle("   LEFT OPEN                        $open")
        }
        settledExamples.forEach { logger.lifecycle("     both agreed, but not with reference: $it") }
        if (settledExamples.isNotEmpty()) {
            logger.lifecycle("       ↳ Both agents gave the same answer, but it differs from the reference.")
            logger.lifecycle("         Maybe the spec clearly chose another valid answer. Or maybe both agents")
            logger.lifecycle("         made the same guess. Read the spec to find out.")
        }
        openExamples.forEach { logger.lifecycle("     open: $it") }
        if (openExamples.isNotEmpty()) {
            logger.lifecycle("       ↳ The agents chose different answers. Check whether the spec leaves this")
            logger.lifecycle("         open on purpose; section 4 should explain that.")
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
