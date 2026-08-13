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
 *  - **both read it the same way, and not ours** — A and B agree with each other and differ from the
 *    reference. Do **not** read this as "the specification decided it differently": it also happens
 *    when the text says nothing and both readers reach for the same default. Measured: on
 *    llama3.2-3b's specification both agents returned −50 for `Percentage(150)` on a price of 100
 *    and both overflowed at `Int.MAX_VALUE` — that text mentions neither clamping nor arithmetic
 *    width, so the agreement came from a shared habit, not from the page. Only reading the sentence
 *    separates the two, and this label deliberately no longer pretends otherwise.
 *  - **left open** — A and B differ. The text did not decide, and each agent decided for itself.
 *    This is the number the reference-agreement score cannot see.
 *
 * A specification is not better for having fewer open cases in every situation — deliberately
 * leaving something free is a legitimate choice, and section 4 of the template exists to record it.
 * What this measures is how much is open *at all*; whether that was on purpose is in the text.
 *
 * ### The two runs have to come from DIFFERENT agents
 *
 * Measured, and it went against the intuition: on a specification with no rounding rule, no
 * compounding rule and a self-contradictory bonus,
 *
 * ```
 * two sessions of the same agent    0 open / 4021
 * two different agents            674 open / 4021
 * ```
 *
 * The two sessions wrote genuinely different code and produced byte-identical behaviour, so the same
 * agent twice reported the same clean zero it reports for the best specification in the corpus.
 *
 * "Left open" is relative to a **population of readers**, and two sessions of one model are one
 * reader twice: the gaps close identically because whatever closes them is identical — both sessions
 * reached for `coerceAtLeast(0)` and for `Long` arithmetic although the text mentions neither.
 *
 * ### The second reader has to be strong, and here is what happens when it is not
 *
 * `qwen2.5-coder:14b` as the second reader reported **391** open cases on the compression of
 * `written/claude-code.md`, against 0 on the long version — which looked like a real result about
 * compression narrowing the set of readers for whom a text is determinate. It was not. Re-measured
 * with a frontier second reader, the same compression leaves **0 of 4021** open.
 *
 * The 391 were the weak reader's incapacity: it overflowed at the top of the `Int` range, and it is
 * the same model that elsewhere in this corpus imported `DiscountRule` from the wrong package and
 * failed to compile at all. A reader whose failures are about itself contributes noise that reads
 * exactly like ambiguity in the text.
 *
 * So the rule is not "use two readers" but **use two readers competent at the task**. The number this
 * task prints is only as trustworthy as the weaker of the pair.
 *
 * So: **agreement between two runs of one agent proves nothing.** Disagreement still proves
 * ambiguity, which makes the same-agent pair a lower bound and never a clean bill of health. This
 * task does not know which pair it was given, so it says so in the footer rather than pretending.
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
        if (a.substringBeforeLast('-') == b.substringBeforeLast('-')) {
            logger.lifecycle("")
            logger.lifecycle("NOTE — these look like two runs of the SAME agent. Disagreement below still")
            logger.lifecycle("proves the text is ambiguous, but AGREEMENT PROVES NOTHING: one model twice is")
            logger.lifecycle("one reader. A thin specification measured this way scored 0 open, and 674 with")
            logger.lifecycle("a second, different agent. Use two different agents for the real number.")
        }

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
        logger.lifecycle("   agreed, same as the reference    $settled")
        logger.lifecycle("   agreed, differs from reference   $differentlySettled")
        logger.lifecycle("   LEFT OPEN                        $open")
        settledExamples.forEach { logger.lifecycle("     agreed, not ours: $it") }
        if (settledExamples.isNotEmpty()) {
            logger.lifecycle("       ↳ both readers landed on the same answer and neither matched us. That is")
            logger.lifecycle("         either the text deciding it differently — fine, ours is one legal answer —")
            logger.lifecycle("         or the text saying NOTHING and both defaulting the same way. Read it.")
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
