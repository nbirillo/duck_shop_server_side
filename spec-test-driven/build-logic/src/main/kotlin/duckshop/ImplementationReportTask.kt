package duckshop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Checks what happens when an agent builds the feature using only your specification.
 * What does its implementation get wrong, or simply do differently?
 *
 * The agent sees the specification and the data types, but not the reference code,
 * the property catalog, or the original business brief. That's important: if it saw
 * the brief, it could fill in gaps that your specification left behind.
 *
 * ### A difference is not automatically a mistake
 *
 * When the implementation disagrees with the reference, there are three possibilities:
 *
 * - Your spec made a decision, but the agent misunderstood it.
 * - Your spec intentionally left a choice open, and both answers are valid.
 * - Your spec chose a different valid answer from the reference, and the agent followed it.
 *
 * The report cannot tell which happened. You need to read the specification.
 * That's why differences are shown, not silently treated as wrong answers.
 *
 * ### Only check what the student was given
 *
 * The basic brief doesn't include `BestOf` or `OnlyIf`. Tests about those rules
 * don't count against a basic-tier specification. The key lists these as `outOfScope`.
 *
 * Params: `-Pagent=<name>`, `[-Pkey=<key.json>]`.
 */
abstract class ImplementationReportTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val agent: Property<String>

    @get:Input
    abstract val keyPath: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val who = agent.orNull ?: providers.gradleProperty("agent").orNull
        ?: error("Missing -Pagent=<name>. Choose the agent run to report on.")
        val dir = root.resolve("implementations/$who")
        require(dir.isDirectory) {
            "No implementations found in $dir. Run: ./gradlew runAgent -Pmode=impl-from-spec " +
                    "-Pagent=$who -Pspec=<a specification>"
        }
        val key = Json.parseToJsonElement(root.resolve(keyPath.get()).readText()).jsonObject
        val claims = key.getValue("_claims").jsonArray.map { it.jsonPrimitive.content }
        // Example: "written/claude-code.md" becomes the module folder "written-claude-code".
        val scope = key.getValue("fixtures").jsonObject.entries.associate { (path, entry) ->
            path.removeSuffix(".md").replace('/', '-') to
                    entry.jsonObject["outOfScope"]?.jsonArray?.map { it.jsonPrimitive.content.toInt() }
                        .orEmpty()
        }
        logger.lifecycle("")
        logger.lifecycle("Implementations built from specifications — $who")
        logger.lifecycle("The agent only had each specification and the data types to work with.")

        val scored = mutableListOf<Triple<String, Int, Int>>()
        dir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { module ->
            val results = module.resolve("build/test-results/test")
            val failed = failingTests(results)
            logger.lifecycle("")
            if (failed == null) {
                logger.lifecycle("── ${module.name} — DID NOT BUILD OR RUN")
                logger.lifecycle("   The agent did not produce a working test run from this specification.")
                return@forEach
            }
            val out = scope[module.name] ?: emptyList()
            val inTier = claims.indices.map { it + 1 }.filterNot { it in out }
            val diverged = inTier.filter { claims[it - 1] in failed }
            scored += Triple(module.name, inTier.size - diverged.size, inTier.size)

            logger.lifecycle("── ${module.name} — ${inTier.size - diverged.size}/${inTier.size} in scope")
            diverged.forEach { logger.lifecycle("   DIVERGES  #$it ${claims[it - 1]}") }
            val ignored = out.filter { claims[it - 1] in failed }
            if (ignored.isNotEmpty()) {
                logger.lifecycle(
                    "   not counted (outside this brief): ${ignored.joinToString { "#$it" }}",
                )
            }
        }
        logger.lifecycle("")
        if (scored.isEmpty()) {
            logger.lifecycle("No implementations could be scored.")
            return
        }
        logger.lifecycle("Agreement with the reference, for claims in scope:")
        scored.sortedByDescending { it.second }.forEach { (name, ok, total) ->
            logger.lifecycle("   ${name.padEnd(scored.maxOf { it.first.length } + 2)}$ok/$total")
        }
        logger.lifecycle("")
        logger.lifecycle("Check each difference before calling it a mistake. The agent and reference may")
        logger.lifecycle("disagree because the spec was unclear, left a choice open, or made a different")
        logger.lifecycle("valid choice on purpose. Read the spec to find out which.")
    }
}
