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
 * Checks the parts of a specification that we can measure without an AI model.
 * This is the first layer of the spec-writing exercise.
 *
 * It checks whether the expected sections are present and filled in, counts the text,
 * and looks for names from the API surface. It cannot tell whether a claim is true,
 * testable, or based on an acknowledged assumption. That needs further review.
 *
 * We also point out topics mentioned both as settled behavior and as open questions.
 * This is a heads-up, not a score. A good spec may make a temporary decision while
 * clearly saying the business still needs to confirm it. Earlier keyword checks
 * mistakenly marked strong specs as weak, so we don't treat overlap as a failure.
 */
abstract class SpecReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Check one SPEC.md file or scan a directory for specifications. */
    @get:Input
    @get:Optional
    abstract val specPath: Property<String>

    /**
     * Use the brief the student actually received to decide which API names to check.
     * Don't use the whole codebase: if the brief never mentioned `BestOf`, the student
     * shouldn't lose credit for leaving it out.
     */
    @get:Input
    abstract val surfaceFile: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val target = root.resolve(
            specPath.orNull ?: providers.gradleProperty("spec").orNull
            ?: error("Please provide -Pspec=<SPEC.md or a directory of specs>"),
        )
        val specs = when {
            target.isFile -> listOf(target)
            target.isDirectory -> target.walkTopDown().filter { it.name.endsWith(".md") }
                .filterNot { it.name.startsWith("README") || it.name.contains("template", ignoreCase = true) }
                .sortedBy { it.path }.toList()
            else -> error("Could not find: $target")
        }
        require(specs.isNotEmpty()) { "No specification files found in $target" }

        val surface = surfaceNames(root.resolve(surfaceFile.get()))
        logger.lifecycle("")
        logger.lifecycle("Specification check — simple checks only, no AI model")
        logger.lifecycle("API surface: ${surface.size} names from ${surfaceFile.get().substringAfterLast('/')}")

        // A spec can span several files. With -Pparts, check them together rather than
        // expecting every file to contain every section and API name.
        // Keep this opt-in: a directory can also hold several independent specs.
        // For example, fixtures/write-spec/written contains six different authors' work.
        // Without -Pparts, each Markdown file is checked on its own.
        val split = specs.size > 1 && providers.gradleProperty("parts").isPresent
        if (split) {
            logger.lifecycle("")
            logger.lifecycle("${specs.size} files checked together as one specification")
            specs.forEach { f ->
                val t = f.readText()
                logger.lifecycle("   ${f.name} · ${t.lines().size} lines · ${t.split(Regex("\\s+")).size} words")
            }
        }

        val judged: List<Pair<String, String>> = if (split) {
            listOf("all ${specs.size} parts" to specs.joinToString("\n\n") { it.readText() })
        } else {
            specs.map { "${it.parentFile.name}/${it.name}" to it.readText() }
        }

        judged.forEach { (label, text) ->
            val sections = sections(text)
            val missing = REQUIRED.filter { req -> sections.keys.none { it.contains(req, ignoreCase = true) } }
            val empty = sections.filterValues { it.isBlank() }.keys
            val unmentioned = surface.filterNot { mentions(text, it) }
            val collisions = collisions(sections)

            logger.lifecycle("")
            logger.lifecycle("── $label")
            logger.lifecycle("   ${text.lines().size} lines · ${text.split(Regex("\\s+")).size} words")
            logger.lifecycle(
                "   sections: " + if (missing.isEmpty() && empty.isEmpty()) "all sections are present and filled in" else
                    (missing.takeIf { it.isNotEmpty() }?.let { "MISSING ${it.joinToString()}" }.orEmpty() +
                            empty.takeIf { it.isNotEmpty() }?.let { " EMPTY ${it.joinToString()}" }.orEmpty()).trim(),
            )
            logger.lifecycle(
                "   surface: " + if (unmentioned.isEmpty()) "every name mentioned" else
                    "not mentioned — ${unmentioned.joinToString()}",
            )
            if (collisions.isNotEmpty()) {
                logger.lifecycle("   mentioned as both settled and open: ${collisions.joinToString()}")
                logger.lifecycle(
                    "     ↳ This is not a failure. It might be a decision made for now, while",
                )
                logger.lifecycle(
                    "       waiting for the business to confirm it. Or it might be a contradiction.",
                )
                logger.lifecycle("       Read those sections to see which it is.")
            }
        }

        logger.lifecycle("")
        logger.lifecycle("Mentioning an API name does not mean you explained it. A filled section can still be wrong.")
        logger.lifecycle("Checking what the specification actually promises is the next step, not this one.")
    }

    /**
     * Check whether [name] appears anywhere, including in ordinary prose.
     * For example, `BigSpenderBonus`, "Big Spender Bonus", and "big-spender bonus"
     * all count. A spec is written for people, so it shouldn't need exact code spelling.
     */
    private fun mentions(text: String, name: String): Boolean {
        val humps = Regex("(?<=[a-z0-9])(?=[A-Z])").split(name)
        val pattern = humps.joinToString("[\\s-]?") { Regex.escape(it) }
        return Regex("\\b$pattern\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
    }

    /** Collect the type, function, and field names the spec is expected to mention. */
    private fun surfaceNames(file: File): List<String> {
        require(file.isFile) { "Could not find the API surface file: $file" }
        val raw = file.readText()
        // For a Markdown brief, check only the Kotlin code blocks it shows.
        val src = if (file.extension == "md") {
            Regex("```kotlin\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
                .findAll(raw).joinToString("\n") { it.groupValues[1] }
                .ifBlank { error("No Kotlin code block found in $file to check the API surface") }
        } else {
            raw
        }
        val types = Regex("""(?:data\s+)?class\s+(\w+)""").findAll(src).map { it.groupValues[1] }
        val funs = Regex("""fun\s+(\w+)\s*\(""").findAll(src).map { it.groupValues[1] }
        val params = Regex("""val\s+(\w+)\s*:""").findAll(src).map { it.groupValues[1] }
        return (types + funs + params).distinct().sorted().toList()
    }

    private fun sections(text: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        val heads = Regex("""^##\s+(.+)$""", RegexOption.MULTILINE).findAll(text).toList()
        heads.forEachIndexed { i, m ->
            val end = if (i + 1 < heads.size) heads[i + 1].range.first else text.length
            out[m.groupValues[1].trim()] = text.substring(m.range.last + 1, end).trim()
        }
        return out
    }

    private fun collisions(sections: Map<String, String>): List<String> {
        fun body(vararg keys: String) = sections.entries
            .filter { e -> keys.any { e.key.contains(it, ignoreCase = true) } }
            .joinToString(" ") { it.value }.lowercase()
        val behaviour = body("behaviour", "behavior")
        val open = body("not specified", "open question")
        if (behaviour.isBlank() || open.isBlank()) return emptyList()
        return TOPICS.filter { (_, rx) -> rx.containsMatchIn(behaviour) && rx.containsMatchIn(open) }.map { it.key }
    }

    private companion object {
        val REQUIRED = listOf("what it does", "behaviour", "edge case", "not specified", "open question")
        val TOPICS = mapOf(
            "rounding" to Regex("round|floor|truncat|fraction|decimal"),
            "order" to Regex("\\border\\b|sequential|applied last"),
            "clamping" to Regex("negative|below zero|clamp"),
            "which price a rule sees" to Regex("shelf price|running price|original price|applied to"),
        )
    }
}

