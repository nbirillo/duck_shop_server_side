package duckshop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject

/**
 * Generates a `solutions/<agent>/` implementation by asking an OpenAI-compatible chat API
 * (Ollama or Mistral) to implement the stub files.
 *
 * The prompt is assembled here from the `:core` given types and the `:starter` stubs only — the
 * reference implementation in `:grading` is never included, so an API agent cannot copy it. The
 * agent may return MULTIPLE files, each in its own `// FILE: <path>` + fenced block.
 *
 * Parameters (project properties):
 *  - `-Pprovider=ollama|mistral`  (required)
 *  - `-Pmodel=<model>`            (required)
 *  - `-Pagent=<name>`             (optional; defaults to `<provider>-<model>`)
 *  - `-Pdry`                      (optional; assemble and print the prompt, skip the HTTP call)
 *
 * Mistral reads the API key from the `MISTRAL_API_KEY` environment variable.
 */
abstract class RunAgentTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    private val packagePath = "org/jetbrains/kotlin/course/duck/shop/admission"
    private val basePackage = packagePath.replace('/', '.')

    @TaskAction
    fun run() {
        val provider = prop("provider") ?: error("Missing -Pprovider=ollama|mistral")
        val model = prop("model") ?: error("Missing -Pmodel=<model>")
        val dry = providers.gradleProperty("dry").isPresent
        val safeModel = model.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val agent = prop("agent") ?: "$provider-$safeModel"

        val root = layout.projectDirectory.asFile
        val stubs = stubFiles(root)
        val systemPrompt = root.resolve("tools/agent-prompt.md").readText()
        val userPrompt = buildUserPrompt(root, stubs)

        val endpoint = when (provider) {
            "ollama" -> "http://localhost:11434/v1/chat/completions"
            "mistral" -> "https://api.mistral.ai/v1/chat/completions"
            else -> error("Unknown provider '$provider' (use ollama|mistral)")
        }

        val agentDir = root.resolve("solutions/$agent")
        if (dry) {
            logger.lifecycle("[runAgent] DRY RUN — no HTTP call, no files written.")
            logger.lifecycle("[runAgent] provider=$provider model=$model agent=$agent endpoint=$endpoint")
            logger.lifecycle("[runAgent] would write ${stubs.size} file(s) under ${agentDir.relativeTo(root)}: ${stubs.map { it.first }}")
            logger.lifecycle("\n===== SYSTEM =====\n$systemPrompt\n===== USER =====\n$userPrompt")
            return
        }

        val authHeader = if (provider == "mistral") {
            "Bearer " + (providers.environmentVariable("MISTRAL_API_KEY").orNull
                ?: error("MISTRAL_API_KEY environment variable is not set"))
        } else null

        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0)
            put("stream", false)
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", systemPrompt) }
                addJsonObject { put("role", "user"); put("content", userPrompt) }
            }
        }

        logger.lifecycle("[runAgent] POST $endpoint (model=$model) ...")
        val request = HttpRequest.newBuilder(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .apply { if (authHeader != null) header("Authorization", authHeader) }
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Chat API returned HTTP ${response.statusCode()}: ${response.body()}")
        }

        val content = Json.parseToJsonElement(response.body())
            .jsonObject["choices"]!!.jsonArray[0]
            .jsonObject["message"]!!.jsonObject["content"]!!.jsonPrimitive.content

        val written = writeSolutionFiles(agentDir, content, stubs.map { it.first })
        agentDir.resolve("build.gradle.kts").writeText("plugins {\n    id(\"duck-shop.solution\")\n}\n")
        agentDir.resolve("agent.json").writeText(
            buildJsonObject {
                put("agent", agent)
                put("provider", provider)
                put("model", model)
                put("checkedOn", LocalDate.now().toString())
                put("temperature", 0)
                put("promptSha256", sha256(systemPrompt + "\n" + userPrompt))
            }.toString() + "\n"
        )

        logger.lifecycle("[runAgent] wrote ${written.size} file(s) to ${agentDir.relativeTo(root)}: $written")
        logger.lifecycle("[runAgent] now run: ./gradlew checkPrimary -PprimaryAgent=$agent")
    }

    private fun prop(name: String): String? = providers.gradleProperty(name).orNull

    /** The stub files under :starter as (relativePath, contents), relative to the base package dir. */
    private fun stubFiles(root: File): List<Pair<String, String>> {
        val starterBase = root.resolve("starter/src/main/kotlin/$packagePath")
        return starterBase.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .map { it.relativeTo(starterBase).path to it.readText() }
            .toList()
    }

    private fun buildUserPrompt(root: File, stubs: List<Pair<String, String>>): String {
        val coreBase = root.resolve("core/src/main/kotlin/$packagePath")
        val given = listOf("Domain.kt", "schedule/TimeTypes.kt")
            .map { coreBase.resolve(it) }
            .filter { it.exists() }
            .joinToString("\n\n") { it.readText() }
        val stubText = stubs.joinToString("\n\n") { (rel, body) -> "// FILE: $rel\n$body" }

        return buildString {
            appendLine("Given types (already on the classpath — do not redeclare):")
            appendLine("```kotlin")
            appendLine(given)
            appendLine("```")
            appendLine()
            appendLine("Implement these stub files. Keep the exact paths, packages and signatures,")
            appendLine("and return each as its own `// FILE:` block per the output contract.")
            appendLine()
            append(stubText)
        }
    }

    /** Parses `// FILE: <path>` + fenced blocks and writes each normalised file. */
    private fun writeSolutionFiles(agentDir: File, content: String, stubPaths: List<String>): List<String> {
        val srcRoot = agentDir.resolve("src/main/kotlin/$packagePath")
        val fileBlock = Regex(
            "//\\s*FILE:\\s*(\\S+)[^\\n]*\\n```(?:kotlin|kt)?\\s*\\n(.*?)```",
            RegexOption.DOT_MATCHES_ALL,
        )
        val matches = fileBlock.findAll(content).toList()

        val units: List<Pair<String, String>> = if (matches.isNotEmpty()) {
            matches.map { it.groupValues[1].trim() to it.groupValues[2] }
        } else {
            // Fallback: the model ignored the // FILE: contract (common with weak models). Merge
            // all fenced code into one file under the stubs' common subpackage. Our stubs share a
            // package, so a single combined file still compiles.
            val allFences = Regex("```(?:kotlin|kt)?\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
                .findAll(content).map { it.groupValues[1] }.toList()
            val combined = if (allFences.isNotEmpty()) allFences.joinToString("\n\n") else content
            val commonSub = stubPaths.map { it.substringBeforeLast('/', "") }.distinct().singleOrNull() ?: ""
            val fallbackRel = if (commonSub.isEmpty()) "Solution.kt" else "$commonSub/Solution.kt"
            logger.warn("[runAgent] no // FILE: markers; merging output into single file $fallbackRel")
            listOf(fallbackRel to combined)
        }

        return units.map { (rel, code) ->
            val sub = rel.substringBeforeLast('/', "").replace('/', '.')
            val pkg = if (sub.isEmpty()) basePackage else "$basePackage.$sub"
            val target = srcRoot.resolve(rel)
            target.parentFile.mkdirs()
            target.writeText(normalizeCode(code, pkg) + "\n")
            rel
        }
    }

    /**
     * Normalises one file's code so the harness stays robust to models that don't honour the
     * output contract: strips any `package`/`// Foo.kt` lines, hoists imports, and prepends the
     * one correct package. Only wrapping is touched, never logic.
     */
    private fun normalizeCode(raw: String, pkg: String): String {
        val lines = raw.trim().lines()
        val imports = lines.map { it.trim() }.filter { it.startsWith("import ") }.distinct()
        val body = lines
            .filterNot { it.trim().startsWith("package ") }
            .filterNot { it.trim().startsWith("import ") }
            .filterNot { it.trim().matches(Regex("//\\s*\\S+\\.kt")) }
            .filterNot { it.trim().matches(Regex("//\\s*FILE:.*")) }
            .filterNot { it.trim().startsWith("```") }
            .joinToString("\n")
            .trim()
        return buildString {
            append("package ").append(pkg).append("\n\n")
            if (imports.isNotEmpty()) append(imports.joinToString("\n")).append("\n\n")
            append(body)
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
