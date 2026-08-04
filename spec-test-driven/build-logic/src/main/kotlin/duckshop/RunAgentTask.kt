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
 * Anthropic). Five modes:
 *  - `impl`  (default)  — implement the :starter stubs; writes to `solutions/<agent>/`.
 *  - `tests`            — write a test suite for the given :core algebra; writes to `test-suites/<agent>/`.
 *  - `verify-exercise`  — author-side: regenerate the flawed starter suite of exercise 11.2 from a
 *    model's real output, with one invalid test planted; overwrites the exercise file.
 *  - `verify-harden`    — the 11.2 task itself: verify and harden that flawed suite. Archived to
 *    `hardened/<agent>/`, which is then scored for validity (`:hardened:<agent>:test`) and for
 *    coverage (`verifyMutants -PmutantTests=hardened/<agent>/src/test/kotlin`).
 *  - `attack`           — the advanced tier of 11.2: given a suite, write an implementation that
 *    PASSES it and still contradicts the specification. Writes to `attacks/<agent>/`, scored by
 *    `verifyAttack -Pagent=<agent>`. Unlike a mutant catalog, an adversary cannot be saturated.
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
            require(it in setOf("impl", "tests", "verify-exercise", "verify-harden", "attack")) {
                "Unknown -Pmode='$it' (use impl|tests|verify-exercise|verify-harden|attack)"
            }
        }
        val dry = providers.gradleProperty("dry").isPresent
        val safeModel = model.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val agent = prop("agent") ?: "$provider-$safeModel"
        val root = layout.projectDirectory.asFile

        val systemPrompt = root.resolve(
            when (mode) {
                "impl" -> "tools/agent-prompt.md"
                "verify-harden" -> "tools/agent-prompt-verify.md"
                "attack" -> "tools/agent-prompt-attack.md"
                else -> "tools/agent-prompt-tests.md" // tests, verify-exercise
            },
        ).readText()
        val stubs = if (mode == "impl") stubFiles(root) else emptyList()
        // Which suite the attack has to get past. An attack is always aimed at one specific suite,
        // so the path is baked into the generated module rather than re-read at verification time.
        val attackSuite = prop("mutantTests")
            ?.takeIf { it != "learner" }
            ?: "exercises/write-tests/src/test/kotlin"
        val userPrompt = when (mode) {
            "impl" -> buildImplPrompt(root, stubs)
            "verify-harden" -> buildVerifyPrompt(root)
            "attack" -> buildAttackPrompt(root, attackSuite)
            else -> buildTestsPrompt(root)
        }
        val outDir = root.resolve(
            when (mode) {
                "tests" -> "test-suites/$agent"
                // Archived per agent (instead of overwriting the exercise) so the same suite can be
                // re-scored later — e.g. against the mutant set — without re-running the model.
                "verify-harden" -> "hardened/$agent"
                "attack" -> "attacks/$agent"
                else -> "solutions/$agent"
            },
        )
        val exerciseFile = root.resolve("exercises/write-tests/src/test/kotlin/$packagePath/PolicyTests.kt")

        val endpoint = when (provider) {
            "ollama" -> "http://localhost:11434/v1/chat/completions"
            "mistral" -> "https://api.mistral.ai/v1/chat/completions"
            "anthropic" -> "https://api.anthropic.com/v1/chat/completions" // OpenAI-compatible layer
            else -> error("Unknown provider '$provider' (use ollama|mistral|anthropic)")
        }

        if (dry) {
            logger.lifecycle("[runAgent] DRY RUN — no HTTP call, no files written.")
            logger.lifecycle("[runAgent] mode=$mode provider=$provider model=$model agent=$agent endpoint=$endpoint")
            val target = if (mode == "verify-exercise") exerciseFile.relativeTo(root) else outDir.relativeTo(root)
            logger.lifecycle("[runAgent] would write under $target")
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

        if (mode == "verify-exercise") {
            val suite = injectPlantedDefect(normalizeCode(extractCode(content), basePackage))
            exerciseFile.parentFile.mkdirs()
            exerciseFile.writeText(suite + "\n")
            logger.lifecycle("[runAgent] wrote exercise starter ${exerciseFile.relativeTo(root)} from $model, with a planted invalid Not test.")
            logger.lifecycle("[runAgent] review: git diff -- ${exerciseFile.relativeTo(root)} ; then ./gradlew :exercises:write-tests:test (should be RED on the planted test).")
            return
        }

        val written = when (mode) {
            "tests" -> writeTestSuite(outDir, content)
            "verify-harden" -> writeTestSuite(outDir, content, fileName = "PolicyTests.kt")
            "attack" -> writeSolutionFiles(outDir, content, emptyList(), nameSuffix = "Attack")
            else -> writeSolutionFiles(outDir, content, stubs.map { it.first })
        }
        outDir.resolve("build.gradle.kts").writeText(
            when (mode) {
                "impl" -> "plugins {\n    id(\"duck-shop.solution\")\n}\n"
                "attack" -> attackBuildScript(
                    coreBase = root.resolve("core/src/main/kotlin/$packagePath"),
                    attackBase = outDir.resolve("src/main/kotlin/$packagePath"),
                    defaultSuite = attackSuite,
                ) { logger.warn("[runAgent] $it") }
                else -> CONSUMER_BUILD_SCRIPT
            },
        )
        outDir.resolve("agent.json").writeText(
            buildJsonObject {
                put("agent", agent)
                put("mode", mode)
                put("provider", provider)
                put("model", model)
                put("checkedOn", LocalDate.now().toString())
                if (mode == "attack") put("attackedSuite", attackSuite)
                put("temperature", 0)
                put("promptSha256", sha256(systemPrompt + "\n" + userPrompt))
            }.toString() + "\n",
        )

        val next = when (mode) {
            "tests" -> "./gradlew :test-suites:$agent:test   (runs the generated tests against :core)"
            "attack" -> "./gradlew verifyAttack -Pagent=$agent   (does the suite catch it, and does it " +
                "really differ from :core?)"
            "verify-harden" -> "./gradlew :hardened:$agent:test   (validity on :core), then " +
                "./gradlew verifyMutants -PmutantTests=hardened/$agent/src/test/kotlin --continue   (mutation score)"
            else -> "./gradlew checkPrimary -PprimaryAgent=$agent"
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

    private fun buildVerifyPrompt(root: File): String {
        val coreBase = root.resolve("core/src/main/kotlin/$packagePath")
        val algebra = listOf("Domain.kt", "Leaves.kt", "Combinators.kt")
            .map { coreBase.resolve(it) }
            .filter { it.exists() }
            .joinToString("\n\n") { it.readText() }
        val flawed = root.resolve("exercises/write-tests/src/test/kotlin/$packagePath/PolicyTests.kt").readText()
        return buildString {
            appendLine("The algebra under test (given and correct — do not redeclare):")
            appendLine("```kotlin")
            appendLine(algebra)
            appendLine("```")
            appendLine()
            appendLine("A test suite another AI wrote for it:")
            appendLine("```kotlin")
            appendLine(flawed)
            appendLine("```")
            appendLine()
            append("Verify and harden it, per the output contract.")
        }
    }

    /**
     * The attack prompt: the algebra as it really is, plus the suite the attack has to get past.
     * Handing over the correct sources is deliberate — they are given to the learner too, and an
     * adversary that cannot see what the right answer is cannot aim at the gaps around it. What it
     * never sees is a mutant catalog, which would name the defects we happen to care about.
     */
    private fun buildAttackPrompt(root: File, suitePath: String): String {
        val coreBase = root.resolve("core/src/main/kotlin/$packagePath")
        val algebra = listOf("Domain.kt", "Leaves.kt", "Combinators.kt")
            .map { coreBase.resolve(it) }
            .filter { it.exists() }
            .joinToString("\n\n") { "// FILE: ${it.name}\n${it.readText()}" }
        val suiteDir = root.resolve(suitePath)
        val suite = suiteDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .joinToString("\n\n") { it.readText() }
        require(suite.isNotBlank()) { "No test sources found under $suitePath — nothing to attack." }

        return buildString {
            appendLine("The specification, as the reference implementation states it:")
            appendLine("```kotlin")
            appendLine(algebra)
            appendLine("```")
            appendLine()
            appendLine("The suite you have to pass:")
            appendLine("```kotlin")
            appendLine(suite)
            appendLine("```")
            appendLine()
            append("Rewrite whichever of the files above you need, per the output contract.")
        }
    }


    /**
     * Parses `// FILE: <path>` + fenced blocks and writes each normalised file (impl and attack).
     *
     * [nameSuffix] renames the file on disk without changing the path reported back. An attack module
     * excludes the `:core` file it replaces, and that exclude applies to every source dir in the set —
     * so a replacement stored under its original name would be excluded along with the original and
     * the class would simply vanish. Writing it as `LeavesAttack.kt` keeps both the exclude and the
     * replacement working, exactly as the mutant generator does with `LeavesMutated.kt`.
     */
    private fun writeSolutionFiles(
        agentDir: File,
        content: String,
        stubPaths: List<String>,
        nameSuffix: String = "",
    ): List<String> {
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
            val onDisk = if (nameSuffix.isEmpty()) rel else rel.removeSuffix(".kt") + nameSuffix + ".kt"
            val target = srcRoot.resolve(onDisk)
            target.parentFile.mkdirs()
            target.writeText(normalizeCode(code, pkg) + "\n")
            rel
        }
    }

    /** Writes the generated test file into src/test (tests and verify-harden modes). */
    private fun writeTestSuite(
        outDir: File,
        content: String,
        fileName: String = "GeneratedPolicyTests.kt",
    ): List<String> {
        val fences = Regex("```(?:kotlin|kt)?\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            .findAll(content).map { it.groupValues[1] }.toList()
        val code = if (fences.isNotEmpty()) fences.joinToString("\n\n") else content
        val rel = fileName
        val target = outDir.resolve("src/test/kotlin/$packagePath/$rel")
        target.parentFile.mkdirs()
        target.writeText(normalizeCode(code, basePackage) + "\n")
        return listOf("src/test/kotlin/$packagePath/$rel")
    }

    /** All fenced Kotlin blocks concatenated (or the whole reply if there are no fences). */
    private fun extractCode(content: String): String {
        val fences = Regex("```(?:kotlin|kt)?\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            .findAll(content).map { it.groupValues[1] }.toList()
        return if (fences.isNotEmpty()) fences.joinToString("\n\n") else content
    }

    /**
     * Injects one guaranteed-invalid test (a wrong `Not` assertion) into a generated suite, so the
     * verify-exercise always has at least one defect for the learner to find — regardless of model.
     */
    private fun injectPlantedDefect(code: String): String {
        val withImports = ensureImports(code, listOf("import kotlin.test.Test", "import kotlin.test.assertTrue"))
        val defect = "\n" +
            "    @Test\n" +
            "    fun `Not returns the decision of the wrapped policy`() {\n" +
            "        // PLANTED DEFECT: Not must INVERT the wrapped policy — this assertion is wrong on purpose.\n" +
            "        assertTrue(Not(KotlinOnly()).admits(Duck(name = \"Kotlina\", price = 40, hasKotlinAttribute = true)))\n" +
            "    }\n"
        val i = withImports.lastIndexOf('}')
        return if (i < 0) withImports + defect else withImports.substring(0, i) + defect + "}" + withImports.substring(i + 1)
    }

    private fun ensureImports(code: String, needed: List<String>): String {
        var out = code
        for (imp in needed) {
            if (!out.contains(imp)) {
                val pkgLineEnd = out.indexOf('\n', out.indexOf("package "))
                out = if (pkgLineEnd < 0) "$imp\n$out"
                else out.substring(0, pkgLineEnd + 1) + imp + "\n" + out.substring(pkgLineEnd + 1)
            }
        }
        return out
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
