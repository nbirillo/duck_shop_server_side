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
 * Scores the **extractor** — layer 2 — against the hand-authored key. Teacher-only.
 *
 * The five-run agreement we measured earlier is reproducibility, not accuracy: `runAgent` calls the
 * model at temperature 0, so identical answers are the expected outcome and say nothing about
 * whether the answers are right. This task supplies the missing half. It reads
 * `fixtures/11.4/key.json`, reads whatever `runAgent -Pmode=spec-extract` wrote under
 * `extractions/<agent>/`, and reports where the two disagree.
 *
 * Two errors are counted apart, because they cost different things:
 *
 *  - a **miss** — the key says the spec asserts a claim, the extractor says it does not. This is the
 *    one that harms a learner: it reports a gap that is not there, in work that is correct.
 *  - a **false claim** — the extractor credits a claim the text never makes. This flatters, and
 *    layer 3 catches it anyway when the property runs and the assertion turns out to be unsupported.
 *
 * A claim the key marks `contra` is one the specification actively gets wrong. The extractor's `NO`
 * is right about the absence and blind to the contradiction, so those are reported separately rather
 * than folded into either count — see `_why_three` in the key.
 *
 * Params: `-Pagent=<name>` (which extraction run to score), `[-Pkey=<path to key.json>]`.
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
            ?: error("Missing -Pagent=<name> — which extraction run to score")
        val keyFile = root.resolve(keyPath.get())
        require(keyFile.isFile) { "Key not found: $keyFile" }

        val key = Json.parseToJsonElement(keyFile.readText()).jsonObject
        val claims = key.getValue("_claims").jsonArray.map { it.jsonPrimitive.content }
        val fixtures = key.getValue("fixtures").jsonObject
        checkAgainstCatalog(root, claims)

        val extractionDir = root.resolve("extractions/$who")
        require(extractionDir.isDirectory) {
            "No extractions under $extractionDir — run: ./gradlew runAgent -Pmode=spec-extract " +
                "-Pagent=$who -Pspec=<fixture>"
        }

        logger.lifecycle("")
        logger.lifecycle("Extraction accuracy — $who against ${keyFile.name}")
        logger.lifecycle("${claims.size} claims, key written by hand from the text of each specification")

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
                    "The property catalog changed and the key was not renumbered with it."
            }
            // The subdirectory is part of the filename on purpose — see writeExtraction. Two
            // fixtures share the basename claude-code.md and must not share an extraction.
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
                        lines += "   #${i + 1} MISS — key: asserted (${whereFor(obj, i + 1)}), extractor: no"
                    }
                    want == "no" && !got -> agreed++
                    want == "no" && got -> {
                        falseClaims += "$name #${i + 1}"
                        lines += "   #${i + 1} FALSE CLAIM — the text does not say it, the extractor says it does"
                    }
                    // "contra": the extractor cannot express "the spec says the opposite", so NO is
                    // the closest it can get. Right about the absence, blind to the error.
                    want == "contra" && !got -> {
                        agreed++
                        blindToContra += "$name #${i + 1}"
                    }
                    else -> {
                        falseClaims += "$name #${i + 1}"
                        lines += "   #${i + 1} FALSE CLAIM — the text asserts the OPPOSITE " +
                            "(${whereFor(obj, i + 1)}) and the extractor credited it"
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
            logger.lifecycle("Not scored — no extraction on file for ${absent.size}: ${absent.joinToString()}")
        }
        if (scored == 0) {
            logger.lifecycle("Nothing scored. Run the extractor over the fixtures first.")
            return
        }
        logger.lifecycle("Agreement with the key: $agreed/$scored (${agreed * 100 / scored}%)")
        logger.lifecycle("  misses (a real claim reported as absent): ${misses.size}${list(misses)}")
        logger.lifecycle("  false claims (credited but not in the text): ${falseClaims.size}${list(falseClaims)}")
        if (blindToContra.isNotEmpty()) {
            logger.lifecycle("")
            logger.lifecycle(
                "Counted as agreement but worth knowing: ${blindToContra.size} claim(s) the " +
                    "specification actively CONTRADICTS${list(blindToContra)}",
            )
            logger.lifecycle(
                "  The extractor answers a yes/no question, so it can only say 'not asserted'. It is",
            )
            logger.lifecycle(
                "  right, and it is silent about the more serious fault. A reader is not.",
            )
        }
        logger.lifecycle("")
        logger.lifecycle("This is the accuracy of the CHECKER, not of any specification. A miss here is the")
        logger.lifecycle("checker telling a learner something is missing from work where it is present — which is")
        logger.lifecycle("why no grade rests on this layer, and why a learner may override it with their own")
        logger.lifecycle("claim list and let the property layer settle it without a model in the loop.")
    }

    private fun list(items: List<String>) = if (items.isEmpty()) "" else " — ${items.joinToString()}"

    /**
     * The key is a list of verdicts positioned by claim number, and the extractor takes its claim
     * numbers from the test names in the catalog. Reorder or rename a test and every verdict in the
     * key silently shifts onto the wrong claim — a scored benchmark that measures nothing. Cheap to
     * check, so it is checked on every run rather than trusted.
     */
    private fun checkAgainstCatalog(root: File, claims: List<String>) {
        val catalog = root.resolve(
            providers.gradleProperty("propertyFile").getOrElse(
                "pricing-properties/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/PricingProperties.kt",
            ),
        )
        if (!catalog.isFile) {
            logger.warn("Catalog not found at $catalog — key/claim agreement not verified.")
            return
        }
        val actual = Regex("fun `([^`]+)`").findAll(catalog.readText()).map { it.groupValues[1] }.toList()
        require(actual == claims) {
            "The key's claim list no longer matches ${catalog.name}.\n" +
                "  catalog: ${actual.joinToString("\n           ")}\n" +
                "  key:     ${claims.joinToString("\n           ")}\n" +
                "Renumber the verdicts in the key by hand — they are positional, and a shifted key " +
                "scores every fixture against the wrong claims while looking perfectly healthy."
        }
    }

    private fun whereFor(entry: kotlinx.serialization.json.JsonObject, claim: Int): String =
        entry["where"]?.jsonObject?.get(claim.toString())?.jsonPrimitive?.content ?: "see the key"

    /**
     * Reads `N: YES|NO` lines. A `NO*` — the extractor's marker for "contradicted" — counts as NO;
     * it produced one of those on a claim about a rule kind the basic brief never mentions, so the
     * marker is not yet trustworthy enough to score on.
     */
    private fun readVerdicts(file: File, expected: Int, path: String): List<Boolean> {
        val found = Regex("""^\s*(\d+)\s*:\s*(\w+)""", RegexOption.MULTILINE)
            .findAll(file.readText())
            .associate { it.groupValues[1].toInt() to it.groupValues[2].uppercase().startsWith("Y") }
        require(found.size == expected) {
            "$path: the extraction has ${found.size} verdicts, the key expects $expected. " +
                "Re-run the extractor on this fixture — a short answer is a failed run, not a result."
        }
        return (1..expected).map { found.getValue(it) }
    }
}
