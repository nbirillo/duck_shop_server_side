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
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject

/**
 * Generates a `solutions/<agent>/` implementation by asking an OpenAI-compatible chat API
 * (Ollama or Mistral) to fill in the stub policy types.
 *
 * The prompt is assembled here from the `:core` contract and the `:starter` stubs only — the
 * reference implementation in `:grading` is never included, so an API agent cannot copy it.
 *
 * Parameters (project properties):
 *  - `-Pprovider=ollama|mistral`  (required)
 *  - `-Pmodel=<model>`            (required, e.g. `qwen2.5-coder` or `mistral-small-latest`)
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
    private val packageName = packagePath.replace('/', '.')

    @TaskAction
    fun run() {
        val provider = prop("provider") ?: error("Missing -Pprovider=ollama|mistral")
        val model = prop("model") ?: error("Missing -Pmodel=<model>")
        val dry = providers.gradleProperty("dry").isPresent
        val safeModel = model.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val agent = prop("agent") ?: "$provider-$safeModel"

        val root = layout.projectDirectory.asFile
        val systemPrompt = root.resolve("AGENTS.md").readText()
        val userPrompt = buildUserPrompt(root)

        val endpoint = when (provider) {
            "ollama" -> "http://localhost:11434/v1/chat/completions"
            "mistral" -> "https://api.mistral.ai/v1/chat/completions"
            else -> error("Unknown provider '$provider' (use ollama|mistral)")
        }

        val agentDir = root.resolve("solutions/$agent")
        if (dry) {
            logger.lifecycle("[runAgent] DRY RUN — no HTTP call, no files written.")
            logger.lifecycle("[runAgent] provider=$provider model=$model agent=$agent endpoint=$endpoint")
            logger.lifecycle("[runAgent] would write: ${agentDir.relativeTo(root)}/{build.gradle.kts, agent.json, src/main/kotlin/$packagePath/Solution.kt}")
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
        val code = extractSolution(content)

        val srcDir = agentDir.resolve("src/main/kotlin/$packagePath")
        srcDir.mkdirs()
        srcDir.resolve("Solution.kt").writeText(code + "\n")
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

        logger.lifecycle("[runAgent] wrote ${agentDir.relativeTo(root)}. Now run: ./gradlew checkPrimary -PprimaryAgent=$agent")
    }

    private fun prop(name: String): String? = providers.gradleProperty(name).orNull

    private fun buildUserPrompt(root: java.io.File): String {
        val coreDir = root.resolve("core/src/main/kotlin/$packagePath")
        val contract = listOf("AdmissionPolicy.kt", "Domain.kt")
            .joinToString("\n\n") { coreDir.resolve(it).readText() }
        val starterDir = root.resolve("starter/src/main/kotlin/$packagePath")
        val stubs = (starterDir.listFiles { f -> f.extension == "kt" } ?: emptyArray())
            .sortedBy { it.name }
            .joinToString("\n\n") { "// ${it.name}\n${it.readText()}" }

        return buildString {
            appendLine("Contract from the :core module (already on the classpath — do not redeclare):")
            appendLine("```kotlin")
            appendLine(contract)
            appendLine("```")
            appendLine()
            appendLine("Stub types to implement (same package):")
            appendLine("```kotlin")
            appendLine(stubs)
            appendLine("```")
            appendLine()
            append("Return one Solution.kt implementing every stub, per the output contract.")
        }
    }

    /**
     * Extracts the Kotlin solution from the model's reply and normalises its formatting so the
     * harness stays robust to weaker models that don't honour the "one file / one package"
     * contract. This touches only wrapping (fences, duplicate `package` headers, `// Foo.kt`
     * file separators, import placement) — never the logic, which is what we want to evaluate.
     */
    private fun extractSolution(content: String): String {
        val fence = Regex("```(?:kotlin|kt)?\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        val blocks = fence.findAll(content).map { it.groupValues[1].trim() }.toList()
        val raw = if (blocks.isNotEmpty()) blocks.joinToString("\n\n") else content.trim()

        val lines = raw.lines()
        val imports = lines.map { it.trim() }.filter { it.startsWith("import ") }.distinct()
        val body = lines
            .filterNot { it.trim().startsWith("package ") }
            .filterNot { it.trim().startsWith("import ") }
            .filterNot { it.trim().matches(Regex("//\\s*\\S+\\.kt")) }
            .joinToString("\n")
            .trim()

        return buildString {
            append("package ").append(packageName).append("\n\n")
            if (imports.isNotEmpty()) append(imports.joinToString("\n")).append("\n\n")
            append(body)
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
