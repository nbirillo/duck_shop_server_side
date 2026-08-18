package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * **Was that a refactor?** (11.5, the last step.)
 *
 * You changed the shape of an implementation and your suite has an opinion. On its own that opinion is
 * ambiguous, and this is the ambiguity the step exists to teach:
 *
 * | behaviour changed | suite red | reading |
 * | --- | --- | --- |
 * | no | no | a clean refactor, and the suite is fine |
 * | no | **yes** | **your suite pinned an accident** — it forbids a rewrite the contract allows |
 * | **yes** | no | **not a refactor at all**, and your suite missed it |
 * | yes | yes | behaviour changed and the suite caught it — fine, but call it what it is |
 *
 * The suite cannot tell you which row you are in, because it is one of the two axes. The probe supplies
 * the other: it records what the implementation *answers* on a fixed corpus, before and after, and a
 * difference there is behaviour changing regardless of what any test thinks.
 *
 * This is exercise 11.2's conformant variants arriving from the other side. There, legal rewrites of
 * somebody else's code were handed to you and a red test was a false alarm. Here you write the rewrite
 * and the suite is yours, which is harder to argue with.
 *
 * **The probe's own tests are not part of "the suite".** They are the instrument; counting them would
 * make the instrument's own corpus guard look like the learner's mistake.
 *
 * Params: `-Pbefore=<run>`, `-Pafter=<run>`, `[-Pexamples=<n>]`.
 */
abstract class RefactorReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val before = providers.gradleProperty("before").orNull
            ?: error("Missing -Pbefore=<the run recorded before you changed anything>")
        val after = providers.gradleProperty("after").orNull
            ?: error("Missing -Pafter=<the run recorded after>")
        require(before != after) { "-Pbefore and -Pafter name the same run ($before); nothing to compare." }
        val examples = providers.gradleProperty("examples").orNull?.toInt() ?: 3

        val specs = (modules(root, before).keys + modules(root, after).keys).sorted()
        require(specs.isNotEmpty()) {
            "No recordings for $before / $after. Collect each state with prepareImplementation and run " +
                "its :test task first."
        }

        logger.lifecycle("")
        logger.lifecycle("Was that a refactor? — $before → $after")

        specs.forEach { spec ->
            val a = modules(root, before)[spec]
            val b = modules(root, after)[spec]
            logger.lifecycle("")
            if (a == null || b == null) {
                logger.lifecycle("── $spec — only one state recorded (${if (a == null) after else before})")
                return@forEach
            }
            report(spec, a, b, examples)
        }
    }

    private fun report(spec: String, a: File, b: File, examples: Int) {
        val ra = a.resolve("build/probe/recording.txt").readLines()
        val rb = b.resolve("build/probe/recording.txt").readLines()
        require(ra.size == rb.size) {
            "$spec: the two recordings are ${ra.size} and ${rb.size} lines, so they came from different " +
                "probe corpora and are not comparable. Re-record both."
        }

        val changed = ra.indices.filter { ra[it] != rb[it] }
        // The probe is the instrument, not the learner's work — its own tests never count as the suite.
        val failedAfter = (failingTests(b.resolve("build/test-results/test")) ?: emptySet())
            .filterNot { it in PROBE_TESTS }
        val ranAfter = everyTest(b.resolve("build/test-results/test")).filterNot { it in PROBE_TESTS }

        logger.lifecycle("── $spec")
        logger.lifecycle("   ${ra.size} probed inputs")
        logger.lifecycle("   behaviour changed on              ${changed.size}")
        logger.lifecycle(
            "   your suite                        " + when {
                ranAfter.isEmpty() -> "you have no tests of your own here"
                failedAfter.isEmpty() -> "green (${ranAfter.size} test(s))"
                else -> "RED — ${failedAfter.joinToString()}"
            },
        )

        changed.take(examples).forEach { i ->
            logger.lifecycle("     ${ra[i].substringBefore(" => ")}")
            logger.lifecycle(
                "       was ${ra[i].substringAfter(" => ")} · now ${rb[i].substringAfter(" => ")}",
            )
        }

        logger.lifecycle("")
        val verdict = when {
            ranAfter.isEmpty() && changed.isEmpty() ->
                "Behaviour is unchanged, so the shape change was safe. Whether your SUITE would have " +
                    "noticed if it had not been is a question you have not asked yet — you have no " +
                    "tests of your own in this module."
            ranAfter.isEmpty() ->
                "Behaviour CHANGED and you have no tests here, so nothing would have told you. This is " +
                    "what a refactor without a suite is: a rewrite you are hoping about."
            changed.isEmpty() && failedAfter.isEmpty() ->
                "A clean refactor. Same answers everywhere, suite green — this is the case worth " +
                    "getting used to, and the only one where both signals agree for the right reason."
            changed.isEmpty() ->
                "★ Behaviour did NOT change and your suite went RED. Your suite is pinning an accident: " +
                    "it forbids a rewrite the contract allows. Read the failing test and ask what it is " +
                    "really asserting — the answer is usually 'how the old code happened to be written'."
            failedAfter.isEmpty() ->
                "★ Behaviour CHANGED and your suite stayed green. That was not a refactor, and your " +
                    "suite missed it. Every changed input above is a test you did not write."
            else ->
                "Behaviour changed and your suite caught it. Nothing is broken — but this was not a " +
                    "refactor, so do not file it as one."
        }
        verdict.chunkedLines(96).forEach { logger.lifecycle("   $it") }
    }

    private fun modules(root: File, agent: String): Map<String, File> =
        (root.resolve("implementations/$agent").listFiles() ?: emptyArray())
            .filter { it.resolve("build/probe/recording.txt").isFile }
            .associateBy { it.name }

    private fun String.chunkedLines(width: Int): List<String> {
        val out = mutableListOf<String>()
        var line = StringBuilder()
        split(" ").forEach { word ->
            if (line.isNotEmpty() && line.length + 1 + word.length > width) {
                out += line.toString(); line = StringBuilder()
            }
            if (line.isNotEmpty()) line.append(' ')
            line.append(word)
        }
        if (line.isNotEmpty()) out += line.toString()
        return out
    }

    private companion object {
        val PROBE_TESTS = setOf("record", "the corpus reaches the values it claims to")
    }
}
