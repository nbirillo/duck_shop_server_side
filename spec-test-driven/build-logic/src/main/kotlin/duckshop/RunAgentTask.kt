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
 * Generates an agent artifact by calling an OpenAI-compatible chat API (Ollama / Mistral /
 * Anthropic). Two modes:
 *  - `impl`  (default) — implement the :starter stubs; writes to `solutions/<agent>/`.
 *  - `tests`           — write a test suite for the given :core algebra; writes to `test-suites/<agent>/`.
 *
 * The prompt is assembled here from :core (and, for impl, the :starter stubs) — never :grading —
 * so an API agent cannot copy a reference. In tests mode the prompt is deliberately generic
 * ("cover every edge case you can think of"): it does NOT enumerate the corner cases, so it
 * measures whether the agent finds them.
 *
 * Params: -Pprovider=ollama|mistral|anthropic, -Pmodel=<m>, [-Pmode=impl|tests], [-Pagent=<name>], [-Pdry].
 * Mistral/Anthropic read their key from MISTRAL_API_KEY / ANTHROPIC_API_KEY.
 */
abstract class RunAgentTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    private val packagePath = "org/jetbrains/kotlin/course/duck/shop/admission"
    private val basePackage = packagePath.replace('/', '.')

    @TaskAction
    fun run() {
        val provider = prop("provider") ?: error("Missing -Pprovider=ollama|mistral|anthropic")
        val model = prop("model") ?: error("Missing -Pmodel=<model>")
        val mode = (prop("mode") ?: "impl").also {
            require(it == "impl" || it == "tests") { "Unknown -Pmode='$it' (use impl|tests)" }
        }
        val dry = providers.gradleProperty("dry").isPresent
        val safeModel = model.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val agent = prop("agent") ?: "$provider-$safeModel"
        val root = layout.projectDirectory.asFile

        val systemPrompt = root.resolve(
            if (mode == "tests") "tools/agent-prompt-tests.md" else "tools/agent-prompt.md",
        ).readText()
        val stubs = if (mode == "impl") stubFiles(root) else emptyList()
        val userPrompt = if (mode == "tests") buildTestsPrompt(root) else buildImplPrompt(root, stubs)
        val outDir = root.resolve(if (mode == "tests") "test-suites/$agent" else "solutions/$agent")

        val endpoint = when (provider) {
            "ollama" -> "http://localhost:11434/v1/chat/completions"
            "mistral" -> "https://api.mistral.ai/v1/chat/completions"
            "anthropic" -> "https://api.anthropic.com/v1/chat/completions" // OpenAI-compatible layer
            else -> error("Unknown provider '$provider' (use ollama|mistral|anthropic)")
        }

        if (dry) {
            logger.lifecycle("[runAgent] DRY RUN — no HTTP call, no files written.")
            logger.lifecycle("[runAgent] mode=$mode provider=$provider model=$model agent=$agent endpoint=$endpoint")
            logger.lifecycle("[runAgent] would write under ${outDir.relativeTo(root)}")
            logger.lifecycle("\n===== SYSTEM =====\n$systemPrompt\n===== USER =====\n$userPrompt")
            return
        }

        val authHeader = when (provider) {
            "mistral" -> "Bearer " + envKey("MISTRAL_API_KEY")
            "anthropic" -> "Bearer " + envKey("ANTHROPIC_API_KEY")
            else -> null
        }

        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0)
            put("stream", false)
            if (provider == "anthropic") put("max_tokens", 4096) // required by the Anthropic API
            putJsonArray("messages") {
                addJsonObject { put("role", "system"); put("content", systemPrompt) }
                addJsonObject { put("role", "user"); put("content", userPrompt) }
            }
        }

        logger.lifecycle("[runAgent] mode=$mode POST $endpoint (model=$model) ...")
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

        val written = if (mode == "tests") {
            writeTestSuite(outDir, content)
        } else {
            writeSolutionFiles(outDir, content, stubs.map { it.first })
        }
        outDir.resolve("build.gradle.kts").writeText(
            if (mode == "tests") CONSUMER_BUILD_SCRIPT else "plugins {\n    id(\"duck-shop.solution\")\n}\n",
        )
        outDir.resolve("agent.json").writeText(
            buildJsonObject {
                put("agent", agent)
                put("mode", mode)
                put("provider", provider)
                put("model", model)
                put("checkedOn", LocalDate.now().toString())
                put("temperature", 0)
                put("promptSha256", sha256(systemPrompt + "\n" + userPrompt))
            }.toString() + "\n",
        )

        val next = if (mode == "tests") {
            "./gradlew :test-suites:$agent:test   (runs the generated tests against :core)"
        } else {
            "./gradlew checkPrimary -PprimaryAgent=$agent"
        }
        logger.lifecycle("[runAgent] wrote ${outDir.relativeTo(root)}: $written")
        logger.lifecycle("[runAgent] now run: $next")
    }

    private fun prop(name: String): String? = providers.gradleProperty(name).orNull

    private fun envKey(name: String): String =
        providers.environmentVariable(name).orNull ?: error("$name environment variable is not set")

    /** The stub files under :starter as (relativePath, contents), relative to the base package dir. */
    private fun stubFiles(root: File): List<Pair<String, String>> {
        val starterBase = root.resolve("starter/src/main/kotlin/$packagePath")
        return starterBase.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .map { it.relativeTo(starterBase).path to it.readText() }
            .toList()
    }

    private fun buildImplPrompt(root: File, stubs: List<Pair<String, String>>): String {
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

    private fun buildTestsPrompt(root: File): String {
        val coreBase = root.resolve("core/src/main/kotlin/$packagePath")
        val given = listOf("Domain.kt", "Leaves.kt", "Combinators.kt")
            .map { coreBase.resolve(it) }
            .filter { it.exists() }
            .joinToString("\n\n") { it.readText() }
        return buildString {
            appendLine("The classes to test (already on the classpath — do not redeclare):")
            appendLine("```kotlin")
            appendLine(given)
            appendLine("```")
            appendLine()
            append("Write one test file for these classes, per the output contract.")
        }
    }

    /** Parses `// FILE: <path>` + fenced blocks and writes each normalised file (impl mode). */
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

    /** Writes the generated test file into src/test (tests mode). */
    private fun writeTestSuite(outDir: File, content: String): List<String> {
        val fences = Regex("```(?:kotlin|kt)?\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            .findAll(content).map { it.groupValues[1] }.toList()
        val code = if (fences.isNotEmpty()) fences.joinToString("\n\n") else content
        val rel = "GeneratedPolicyTests.kt"
        val target = outDir.resolve("src/test/kotlin/$packagePath/$rel")
        target.parentFile.mkdirs()
        target.writeText(normalizeCode(code, basePackage) + "\n")
        return listOf("src/test/kotlin/$packagePath/$rel")
    }

    /**
     * Normalises one file's code so the harness stays robust to models that don't honour the
     * output contract: strips any `package`/`import`/`// Foo.kt`/`// FILE:`/fence lines, hoists
     * imports, and prepends the one correct package. Only wrapping is touched, never logic.
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

    private companion object {
        // Build script for a generated test-suite module (tests mode): a plain module that runs
        // its own tests against the given :core algebra.
        val CONSUMER_BUILD_SCRIPT = """
            plugins {
                kotlin("jvm")
            }

            kotlin {
                jvmToolchain(21)
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation(project(":core"))
                testImplementation(kotlin("test"))
            }

            tasks.test {
                useJUnitPlatform()
            }

        """.trimIndent()
    }
}
