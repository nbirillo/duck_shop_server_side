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
 * The last question 11.4 asks of a specification: **an agent implements the feature from your text
 * alone — what does it get wrong?**
 *
 * The agent is given the specification and the shape of the data. Not the reference, not this
 * catalog, and above all not the original business brief: with the brief in hand a vague
 * specification still produces a correct implementation, because the agent is answering from the
 * brief. What is measured is what the specification alone carries.
 *
 * ### A failing property here is a divergence, not a verdict
 *
 * Three different things produce one, and the report cannot tell them apart:
 *
 *  - the specification **decided** it and the agent read it the other way — the text did not work;
 *  - the specification **deliberately left it open**, so both behaviours conform and there is no
 *    defect at all, only our reference being one of the legal answers;
 *  - the specification decided it **differently from our reference**, and the agent is faithful.
 *
 * Only reading the sentence settles which. Same shape as the conformant variants in 11.2, arriving
 * from the other side, and the same rule follows: report it, never score it silently.
 *
 * ### Out of tier
 *
 * A specification written from the basic brief has never been shown `BestOf` or `OnlyIf`, so the two
 * properties about them are excluded rather than counted as failures — the same `outOfScope` the key
 * uses. Counting them would repeat the mistake the surface check made when it read the codebase
 * instead of the brief.
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
            ?: error("Missing -Pagent=<name> — whose implementations to report on")
        val dir = root.resolve("implementations/$who")
        require(dir.isDirectory) {
            "No implementations under $dir — run: ./gradlew runAgent -Pmode=impl-from-spec " +
                "-Pagent=$who -Pspec=<a specification>"
        }

        val key = Json.parseToJsonElement(root.resolve(keyPath.get()).readText()).jsonObject
        val claims = key.getValue("_claims").jsonArray.map { it.jsonPrimitive.content }
        // fixture path "written/claude-code.md" -> module dir "written-claude-code"
        val scope = key.getValue("fixtures").jsonObject.entries.associate { (path, entry) ->
            path.removeSuffix(".md").replace('/', '-') to
                entry.jsonObject["outOfScope"]?.jsonArray?.map { it.jsonPrimitive.content.toInt() }
                    .orEmpty()
        }

        logger.lifecycle("")
        logger.lifecycle("Implemented from the specification alone — $who")
        logger.lifecycle("Each specification was the agent's only description of the behaviour.")

        val scored = mutableListOf<Triple<String, Int, Int>>()
        dir.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }?.forEach { module ->
            val results = module.resolve("build/test-results/test")
            val failed = failingTests(results)
            logger.lifecycle("")
            if (failed == null) {
                logger.lifecycle("── ${module.name} — DID NOT BUILD OR NEVER RAN")
                logger.lifecycle("   A specification an agent cannot turn into compiling code is itself a result.")
                return@forEach
            }
            val out = scope[module.name] ?: emptyList()
            val inTier = claims.indices.map { it + 1 }.filterNot { it in out }
            val diverged = inTier.filter { claims[it - 1] in failed }
            scored += Triple(module.name, inTier.size - diverged.size, inTier.size)

            logger.lifecycle("── ${module.name} — ${inTier.size - diverged.size}/${inTier.size} in tier")
            diverged.forEach { logger.lifecycle("   DIVERGES  #$it ${claims[it - 1]}") }
            val ignored = out.filter { claims[it - 1] in failed }
            if (ignored.isNotEmpty()) {
                logger.lifecycle(
                    "   not counted (never in this brief): ${ignored.joinToString { "#$it" }}",
                )
            }
        }

        logger.lifecycle("")
        if (scored.isEmpty()) {
            logger.lifecycle("Nothing scored.")
            return
        }
        logger.lifecycle("In-tier agreement with the reference:")
        scored.sortedByDescending { it.second }.forEach { (name, ok, total) ->
            logger.lifecycle("   ${name.padEnd(scored.maxOf { it.first.length } + 2)}$ok/$total")
        }
        logger.lifecycle("")
        logger.lifecycle("Read every divergence before believing it. It means the agent and the reference")
        logger.lifecycle("disagree — which happens when a specification failed to say something, and equally")
        logger.lifecycle("when it said something different on purpose and the agent obeyed it.")
    }
}
