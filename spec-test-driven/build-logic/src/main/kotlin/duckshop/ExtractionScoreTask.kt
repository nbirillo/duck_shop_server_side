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
import java.io.File
import javax.inject.Inject

/**
 * Checks how accurately the AI extractor (layer 2) reads specifications. Teacher-only.
 *
 * Getting the same answer five times doesn't mean the answer is right. `runAgent` uses
 * temperature 0, so repeated answers are expected. This task checks accuracy instead:
 * it compares the hand-written `fixtures/write-spec/key.json` with the results saved by
 * `runAgent -Pmode=spec-extract` in `extractions/<agent>/`.
 *
 * It looks for two kinds of mistakes:
 *
 * - **Miss:** the spec makes a claim, but the extractor says it doesn't. This can make
 *   a student think their specification is missing something that is already there.
 * - **False claim:** the extractor says the spec makes a claim that isn't in the text.
 *   The property checks in layer 3 can catch unsupported claims later.
 *
 * The key also uses `contra` when a spec says the opposite of what it should.
 * The extractor can answer `NO`, but that doesn't tell us it noticed the contradiction.
 * We show those cases separately. See `_why_three` in the key.
 *
 * Options: `-Pagent=<name>` and, optionally, `-Pkey=<path to key.json>`.
 */
abstract class ExtractionScoreTask @Inject constructor(
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
        ?: error("Missing -Pagent=<name>. Choose the extraction run to check.")
        val keyFile = root.resolve(keyPath.get())
        require(keyFile.isFile) { "Key not found: $keyFile" }

        val key = Json.parseToJsonElement(keyFile.readText()).jsonObject
        val claims = key.getValue("_claims").jsonArray.map { it.jsonPrimitive.content }
        val fixtures = key.getValue("fixtures").jsonObject
        checkAgainstCatalog(root, claims)

        val extractionDir = root.resolve("extractions/$who")
        require(extractionDir.isDirectory) {
            "No extraction results in $extractionDir. First run: ./gradlew runAgent -Pmode=spec-extract " +
                    "-Pagent=$who -Pspec=<fixture>"
        }

        logger.lifecycle("")
        logger.lifecycle("Extractor accuracy — $who compared with ${keyFile.name}")
        logger.lifecycle("${claims.size} claims, checked against a hand-written answer key")

        var scored = 0
        var agreed = 0
        val misses = mutableListOf<String>()
        val falseClaims = mutableListOf<String>()
        val blindToContra = mutableListOf<String>()
        val absent = mutableListOf<String>()

        fixtures.entries.sortedBy { it.key }.forEach { (path, entry) ->
            val obj = entry.jsonObject
            val expected = obj.getValue("verdicts").jsonArray.map { it.jsonPrimitive.content }
            require(expected.size == claims.size) {
                "$path: the key has ${expected.size} verdicts, the catalog has ${claims.size} claims. " +
                        "The catalog may have changed. Update the claim numbers in the key."
            }
            // Keep the subdirectory in the name. Two fixtures are called claude-code.md,
            // and their extraction results must stay separate. See writeExtraction.
            val name = path.removeSuffix(".md")
            val file = extractionDir.resolve("$name.txt")
            if (!file.isFile) {
                absent += path
                return@forEach
            }

            val actual = readVerdicts(file, claims.size, path)
            val lines = mutableListOf<String>()
            expected.forEachIndexed { i, want ->
                val got = actual[i]
                scored++
                when {
                    want == "yes" && got -> agreed++
                    want == "yes" && !got -> {
                        misses += "$name #${i + 1}"
                        lines += "   #${i + 1} MISS — the spec makes this claim (${whereFor(obj, i + 1)}), but the extractor says NO"
                    }
                    want == "no" && !got -> agreed++
                    want == "no" && got -> {
                        falseClaims += "$name #${i + 1}"
                        lines += "   #${i + 1} FALSE CLAIM — the extractor found a claim that isn't in the text"
                    }
                    // For "contra", NO is the closest answer the extractor can give.
                    // It can't say that the spec actually makes the opposite claim.
                    want == "contra" && !got -> {
                        agreed++
                        blindToContra += "$name #${i + 1}"
                    }
                    else -> {
                        falseClaims += "$name #${i + 1}"
                        lines += "   #${i + 1} FALSE CLAIM — the text actually says the opposite " +
                                "(${whereFor(obj, i + 1)}), but the extractor counted the claim"
                    }
                }
            }

            logger.lifecycle("")
            val wrong = lines.size
            logger.lifecycle("── $path — ${expected.size - wrong}/${expected.size}")
            lines.forEach(logger::lifecycle)
        }

        report(scored, agreed, misses, falseClaims, blindToContra, absent)
    }

    private fun report(
        scored: Int,
        agreed: Int,
        misses: List<String>,
        falseClaims: List<String>,
        blindToContra: List<String>,
        absent: List<String>,
    ) {
        logger.lifecycle("")
        if (absent.isNotEmpty()) {
            logger.lifecycle("Skipped ${absent.size} specification(s) with no extraction results: ${absent.joinToString()}")
        }
        if (scored == 0) {
            logger.lifecycle("Nothing to score yet. Run the extractor on the fixtures first.")
            return
        }
        logger.lifecycle("Matches the answer key: $agreed/$scored (${agreed * 100 / scored}%)")
        logger.lifecycle("  missed claims (in the spec, but not found): ${misses.size}${list(misses)}")
        logger.lifecycle("  false claims (not actually in the spec): ${falseClaims.size}${list(falseClaims)}")
        if (blindToContra.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle(
                "Counted as matches, but worth checking: ${blindToContra.size} claim(s) the " +
                        "specification actually CONTRADICTS${list(blindToContra)}",
            )
            logger.lifecycle(
                "  The extractor only answers YES or NO. It can say a claim is missing,",
            )
            logger.lifecycle(
                "  but it cannot explain that the spec says the opposite.",
            )
        }
        logger.lifecycle("")
        logger.lifecycle("This score is about the CHECKER, not the quality of a specification. A miss means")
        logger.lifecycle("the checker overlooked something the student actually wrote. That's why")
        logger.lifecycle("this layer doesn't decide the grade. Students can provide their own claim list")
        logger.lifecycle("and have the property checks verify it without involving a model.")
    }

    private fun list(items: List<String>) = if (items.isEmpty()) "" else " — ${items.joinToString()}"

    /**
     * Claim numbers come from test names in the property catalog. If those tests move
     * or get renamed, the hand-written key may point to the wrong claims.
     * Check the names on every run so we don't report misleading scores.
     */
    private fun checkAgainstCatalog(root: File, claims: List<String>) {
        val catalog = root.resolve(
            providers.gradleProperty("propertyFile").getOrElse(
                "pricing-properties/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/PricingProperties.kt",
            ),
        )
        if (!catalog.isFile) {
            logger.warn("Can't find the catalog at $catalog, so the key's claim list was not checked.")
            return
        }
        val actual = Regex("fun `([^`]+)`").findAll(catalog.readText()).map { it.groupValues[1] }.toList()
        require(actual == claims) {
            "The claims in the key no longer match ${catalog.name}.\n" +
                    "  catalog: ${actual.joinToString("\n           ")}\n" +
                    "  key:     ${claims.joinToString("\n           ")}\n" +
                    "Update the claim numbers in the key by hand. They depend on the order, and " +
                    "the wrong order would give misleading scores for every fixture."
        }
    }

    private fun whereFor(entry: kotlinx.serialization.json.JsonObject, claim: Int): String =
        entry["where"]?.jsonObject?.get(claim.toString())?.jsonPrimitive?.content ?: "see the key"

    /**
     * Read verdicts written as `N: YES|NO`. Treat `NO*` as NO too.
     * The extractor sometimes uses `NO*` incorrectly, so we don't score it separately.
     */
    private fun readVerdicts(file: File, expected: Int, path: String): List<Boolean> {
        val found = Regex("""^\s*(\d+)\s*:\s*(\w+)""", RegexOption.MULTILINE)
            .findAll(file.readText())
            .associate { it.groupValues[1].toInt() to it.groupValues[2].uppercase().startsWith("Y") }
        require(found.size == expected) {
            "$path: found ${found.size} verdicts, but the key expects $expected. " +
                    "Run the extractor again for this fixture. An incomplete answer can't be scored."
        }
        return (1..expected).map { found.getValue(it) }
    }
}
