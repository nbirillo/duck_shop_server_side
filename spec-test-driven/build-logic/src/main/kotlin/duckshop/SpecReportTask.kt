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
 * The deterministic half of checking a specification (exercise 11.4). **No model is involved.**
 *
 * It answers only questions that have a yes/no answer in the text itself: are the template's sections
 * there and filled, how long is it, and does it mention every name in the surface it is supposed to
 * describe. Everything that needs judgement — is a statement checkable, is a claim true, is an
 * assumption acknowledged — is left to the property layer and to a human.
 *
 * One thing here is reported and deliberately **not scored**: a topic that appears both in the
 * behaviour section and in "deliberately not specified" or "open questions". During calibration a
 * keyword scan for exactly that ranked the best spec we had *last*, because the strongest spec
 * cross-lists on purpose — it decides in §2 and records in §5 that the business owns the real answer.
 * A weak spec that never mentions the topic at all comes out clean. The collision is worth a look and
 * is never a verdict, so the report says so in as many words.
 */
abstract class SpecReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** A single `SPEC.md`, or a directory to scan for them. */
    @get:Input
    @get:Optional
    abstract val specPath: Property<String>

    /**
     * What defines the surface a spec has to cover. Point it at **the brief the learner was given**,
     * not at the codebase: if the brief never showed `BestOf`, a basic-tier spec that says nothing
     * about it has no gap, and flagging one would penalise correct work — the same way the keyword
     * collision scan ranked the best spec last during calibration.
     */
    @get:Input
    abstract val surfaceFile: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val target = root.resolve(
            specPath.orNull ?: providers.gradleProperty("spec").orNull
                ?: error("Missing -Pspec=<SPEC.md or a directory of them>"),
        )
        val specs = when {
            target.isFile -> listOf(target)
            target.isDirectory -> target.walkTopDown().filter { it.name.endsWith(".md") }
                .filterNot { it.name.startsWith("README") || it.name.contains("template", ignoreCase = true) }
                .sortedBy { it.path }.toList()
            else -> error("Not found: $target")
        }
        require(specs.isNotEmpty()) { "No specifications under $target" }

        val surface = surfaceNames(root.resolve(surfaceFile.get()))
        logger.lifecycle("")
        logger.lifecycle("Specification check — deterministic only, no model involved")
        logger.lifecycle("Surface to describe: ${surface.size} names from ${surfaceFile.get().substringAfterLast('/')}")

        specs.forEach { file ->
            val text = file.readText()
            val sections = sections(text)
            val missing = REQUIRED.filter { req -> sections.keys.none { it.contains(req, ignoreCase = true) } }
            val empty = sections.filterValues { it.isBlank() }.keys
            val unmentioned = surface.filterNot { name ->
                Regex("\\b${Regex.escape(name)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            }
            val collisions = collisions(sections)

            logger.lifecycle("")
            logger.lifecycle("── ${file.parentFile.name}/${file.name}")
            logger.lifecycle("   ${text.lines().size} lines · ${text.split(Regex("\\s+")).size} words")
            logger.lifecycle(
                "   sections: " + if (missing.isEmpty() && empty.isEmpty()) "all present and filled" else
                    (missing.takeIf { it.isNotEmpty() }?.let { "MISSING ${it.joinToString()}" }.orEmpty() +
                        empty.takeIf { it.isNotEmpty() }?.let { " EMPTY ${it.joinToString()}" }.orEmpty()).trim(),
            )
            logger.lifecycle(
                "   surface: " + if (unmentioned.isEmpty()) "every name mentioned" else
                    "NEVER MENTIONED — ${unmentioned.joinToString()}",
            )
            if (collisions.isNotEmpty()) {
                logger.lifecycle("   also in §4/§5 after being settled in §2: ${collisions.joinToString()}")
                logger.lifecycle(
                    "     ↳ not a verdict. Each one is either an acknowledged assumption (correct — the",
                )
                logger.lifecycle(
                    "       spec decided it and says the business owns the real answer) or a contradiction.",
                )
                logger.lifecycle("       Only reading them apart tells you which.")
            }
        }

        logger.lifecycle("")
        logger.lifecycle("Mentioning a name is not describing it, and a filled section is not a true one.")
        logger.lifecycle("What the claims are worth is the property layer's question, not this one.")
    }

    /** Declared type names and constructor parameter names — the vocabulary a spec has to cover. */
    private fun surfaceNames(file: File): List<String> {
        require(file.isFile) { "Surface file not found: $file" }
        val raw = file.readText()
        // A brief is Markdown: only the Kotlin it shows counts as the surface.
        val src = if (file.extension == "md") {
            Regex("```kotlin\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
                .findAll(raw).joinToString("\n") { it.groupValues[1] }
                .ifBlank { error("No kotlin block in $file to read the surface from") }
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
