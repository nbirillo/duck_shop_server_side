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
            require(it in setOf("impl", "tests", "verify-exercise", "verify-harden", "attack", "spec", "spec-advanced", "spec-compress", "spec-extract", "impl-from-spec")) {
                "Unknown -Pmode='$it' (use impl|tests|verify-exercise|verify-harden|attack|spec|" +
                    "spec-advanced|spec-compress|spec-extract|impl-from-spec)"
            }
        }
        val dry = providers.gradleProperty("dry").isPresent
        val safeModel = model.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val agent = prop("agent") ?: "$provider-$safeModel"
        val root = layout.projectDirectory.asFile

        // The prompts live in the student build; the grading build reaches them across the sibling.
        val promptDir = prop("promptDir") ?: "tools"
        val systemPrompt = root.resolve(
            when (mode) {
                "impl" -> "$promptDir/agent-prompt.md"
                "verify-harden" -> "$promptDir/agent-prompt-verify.md"
                "attack" -> "$promptDir/agent-prompt-attack.md"
                "spec", "spec-advanced" -> "$promptDir/agent-prompt-spec.md"
                "spec-compress" -> "$promptDir/agent-prompt-compress.md"
                "spec-extract" -> "$promptDir/agent-prompt-extract.md"
                "impl-from-spec" -> "$promptDir/agent-prompt-impl-spec.md"
                else -> "$promptDir/agent-prompt-tests.md" // tests, verify-exercise
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
            "spec" -> buildSpecPrompt(root, "briefs/11.4-basic.md", "SPEC-template.md")
            "spec-advanced" -> buildSpecPrompt(root, "briefs/11.4-advanced.md", "SPEC-template-advanced.md")
            "spec-compress" -> buildCompressPrompt(root)
            "spec-extract" -> buildExtractPrompt(root)
            "impl-from-spec" -> buildImplFromSpecPrompt(root)
            else -> buildTestsPrompt(root)
        }
        val outDir = root.resolve(
            when (mode) {
                "tests" -> "test-suites/$agent"
                // Archived per agent (instead of overwriting the exercise) so the same suite can be
                // re-scored later — e.g. against the mutant set — without re-running the model.
                "verify-harden" -> "hardened/$agent"
                "attack" -> "attacks/$agent"
                "spec" -> "specs/$agent"
                "spec-advanced" -> "specs-advanced/$agent"
                "spec-compress" -> "specs-short/$agent"
                "spec-extract" -> "extractions/$agent"
                "impl-from-spec" -> "implementations/$agent/${specKey(prop("spec"))}"
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
            "spec", "spec-advanced", "spec-compress" -> writeSpec(outDir, content)
            "spec-extract" -> writeExtraction(outDir, content, prop("spec")!!)
            "impl-from-spec" -> writeImplementation(outDir, content)
            else -> writeSolutionFiles(outDir, content, stubs.map { it.first })
        }
        // A spec is a document; an extraction is a list of verdicts. Neither is a Gradle module.
        // An implementation IS one — it has to compile and be tested.
        if (mode !in setOf("spec", "spec-advanced", "spec-compress", "spec-extract")) outDir.resolve("build.gradle.kts").writeText(
            when (mode) {
                "impl" -> "plugins {\n    id(\"duck-shop.solution\")\n}\n"
                "impl-from-spec" -> implFromSpecBuildScript()
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
            "spec", "spec-advanced", "spec-compress" -> "read ${outDir.relativeTo(root)}/SPEC.md — at this stage it is judged by eye, not by machine"
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
            // The budget is only meaningful if the agent knows about it while writing. Measuring an
            // unconstrained suite against a ceiling afterwards says how economical agents happen to
            // be; telling them first asks whether they can choose.
            prop("testBudget")?.toIntOrNull()?.let { budget ->
                appendLine()
                appendLine()
                append(
                    "Keep the suite to at most $budget tests. That is a hard limit, so spend them on the " +
                        "cases that tell correct code apart from wrong code, and drop anything that only " +
                        "repeats a distinction another test already makes.",
                )
            }
        }
    }

    /**
     * The spec prompt (11.4): the informal feature description and the template the learner gets,
     * and nothing else. No reference implementation exists yet and none is shown — the point is to
     * see what an agent determines from an under-determined brief.
     */
    /**
     * The corpus-generation prompt (teacher-side). The **brief** is read from this build, not from
     * the student folder: the business text was moved out of `exercises/write-spec/` precisely so an
     * agent pointed at a learner's project cannot find it. What stays there is the signature and the
     * instructions, which is what the surface check and the implement prompt actually read.
     */
    private fun buildSpecPrompt(root: File, briefFile: String, templateFile: String): String {
        val briefFileResolved = root.resolve(prop("brief") ?: briefFile)
        require(briefFileResolved.isFile) {
            "Brief not found: $briefFileResolved — it lives in the grading build (briefs/), not the " +
                "student folder. Run this from spec-test-driven-grading/, or pass -Pbrief=<path>."
        }
        // Everything above the `---` in a brief file is a note to the teacher about why the business
        // text is kept out of the student folder. Sending that to the agent would prompt it with our
        // commentary on the exercise instead of the exercise.
        val brief = briefFileResolved.readText().substringAfter("\n---\n").trim()
        require(brief.isNotBlank()) { "Brief $briefFileResolved has no content after its --- separator." }

        // A learner receives the brief AND the instructions, so the prompt carries both — this is
        // what the whole README used to supply before the business text was moved out, and the
        // committed corpus was generated from exactly that.
        fun student(name: String) = root.resolve("../spec-test-driven/exercises/write-spec/$name")
            .let { if (it.isFile) it else root.resolve("exercises/write-spec/$name") }
            .readText()
        val instructions = student(if (templateFile.contains("advanced")) "README-advanced.md" else "README.md")
        val template = student(templateFile)
        return buildString {
            appendLine("The task, exactly as the learner receives it:")
            appendLine("```markdown")
            appendLine(instructions)
            appendLine("```")
            appendLine()
            appendLine("The brief the learner is given in class:")
            appendLine("```markdown")
            appendLine(brief)
            appendLine("```")
            appendLine()
            appendLine("The template to fill in:")
            appendLine("```markdown")
            appendLine(template)
            appendLine("```")
            appendLine()
            append("Write the specification.")
        }
    }

    /**
     * The extraction prompt (11.4 layer 2). Claim ids come from the property catalog's test names, so
     * the two can never drift: renaming a property renames the claim. This is **matching against the
     * author's rubric**, not understanding a specification, and the prompt says so — a learner cannot
     * be credited for a claim outside the list, which is the price of a check that is comparable
     * between learners.
     */
    private fun buildExtractPrompt(root: File): String {
        val specFile = root.resolve(prop("spec") ?: error("Missing -Pspec=<SPEC.md>"))
        val propsFile = root.resolve(
            prop("propertyFile")
                ?: "../spec-test-driven-grading/pricing-properties/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/PricingProperties.kt",
        )
        val claims = Regex("fun `([^`]+)`").findAll(propsFile.readText()).map { it.groupValues[1] }.toList()
        require(claims.isNotEmpty()) { "No claims found in $propsFile" }
        return buildString {
            appendLine("The claims:")
            claims.forEachIndexed { i, c -> appendLine("${i + 1}. $c") }
            appendLine()
            appendLine("The specification:")
            appendLine("```markdown")
            appendLine(specFile.readText())
            appendLine("```")
            appendLine()
            append("One line per claim, ${claims.size} lines, nothing else.")
        }
    }

    /** Directory name for one spec's implementation: the fixture's parent dir and file name. */
    private fun specKey(specPath: String?): String {
        val path = specPath ?: error("Missing -Pspec=<the specification to implement from>")
        return path.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
            .takeLast(2).joinToString("-")
    }

    /**
     * The 11.4 implement-from-spec prompt: the learner's specification, the shape of the data, and
     * **nothing else**.
     *
     * Three things are deliberately withheld, and each was a real leak before it was closed.
     *
     *  - **The original business brief.** The worst of the three: hand the agent the brief and a
     *    vague specification still produces a correct implementation, because the agent answers from
     *    the brief. What is being measured is what the specification alone conveys.
     *  - **The reference and the property catalog**, for the obvious reason.
     *  - **`:core`'s `DiscountRule.kt` itself.** Its KDoc says in as many words that defining this
     *    behaviour *is* exercise 11.4, and it declares all six rule kinds — so an author working from
     *    the basic brief, which shows three, would be implemented against a surface they never saw.
     *    The declaration comes from **the brief the specification was written against**, exactly as
     *    the surface check does, and for the same reason.
     *
     * The file still compiles against the real six-case type, so the closing note tells the agent
     * what to do with cases the specification does not reach — without saying what they mean.
     */
    private fun buildImplFromSpecPrompt(root: File): String {
        val spec = root.resolve(prop("spec") ?: error("Missing -Pspec=<SPEC.md>"))
        require(spec.isFile) { "Specification not found: $spec" }

        val briefFile = root.resolve(prop("implSurface") ?: DEFAULT_IMPL_SURFACE)
        require(briefFile.isFile) { "Brief not found: $briefFile" }
        val surface = withUnshownCasesNoted(
            root,
            Regex("```kotlin\\s*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
                .findAll(briefFile.readText()).joinToString("\n\n") { it.groupValues[1].trim() }
                .ifBlank { error("No kotlin block in $briefFile to take the surface from") },
        )

        val typeFiles = (prop("implTypes") ?: DEFAULT_IMPL_TYPES).split(',')
            .map { it.trim() }.filter { it.isNotEmpty() }
            .map { root.resolve(it) }
        typeFiles.forEach { require(it.isFile) { "Type source not found: $it" } }

        return buildString {
            appendLine("The data types, already declared and on your compile path:")
            typeFiles.forEach {
                appendLine()
                appendLine("```kotlin")
                appendLine(it.readText().trim())
                appendLine("```")
            }
            appendLine()
            appendLine("The surface to implement:")
            appendLine()
            appendLine("```kotlin")
            appendLine(surface)
            appendLine("```")
            appendLine()
            appendLine("The specification:")
            appendLine()
            appendLine("```markdown")
            appendLine(spec.readText().trim())
            appendLine("```")
            appendLine()
            appendLine(
                "If `DiscountRule` turns out to have cases the specification above does not " +
                    "describe, leave the price unchanged for them so that the file compiles.",
            )
            appendLine()
            append("Return the implementation file, and nothing else.")
        }
    }

    /**
     * A basic-tier brief shows three rule kinds; the real `DiscountRule` is sealed and has six, so a
     * `when` over three of them does not compile. Saying so in a closing sentence did not work —
     * both agents in the first run ignored it and produced a non-exhaustive `when`. The note has to
     * be **inside the declaration**, where the agent is looking when it writes the branches.
     *
     * It names no case and no behaviour, so the advanced tier is not given away; it only says that
     * more exist and that this specification does not reach them. Emitted only when the brief really
     * does show fewer cases than the type has, so the advanced brief gets nothing.
     */
    private fun withUnshownCasesNoted(root: File, surface: String): String {
        val declared = root.resolve(prop("implRuleType") ?: DEFAULT_IMPL_RULE_TYPE)
        if (!declared.isFile) return surface
        fun cases(text: String) =
            Regex("""data class (\w+)""").findAll(text).map { it.groupValues[1] }.toSet()
        val unshown = cases(declared.readText()) - cases(surface)
        if (unshown.isEmpty()) return surface
        val marker = Regex("""(sealed interface DiscountRule \{[\s\S]*?)\n\}""")
        return marker.replace(surface) {
            it.groupValues[1] +
                "\n\n    // ...and ${unshown.size} further case(s) this specification does not" +
                "\n    // describe. Your `when` must still be exhaustive: leave the price unchanged" +
                "\n    // for them.\n}"
        }
    }

    /** Writes the agent's file into the pricing package of a fresh module. */
    private fun writeImplementation(outDir: File, content: String): List<String> {
        val pkg = prop("implPackage") ?: DEFAULT_IMPL_PACKAGE
        // Drop anything from a previous run first, so a shorter answer cannot leave stale
        // declarations behind and quietly keep the module compiling.
        outDir.resolve("src").deleteRecursively()
        val target = outDir.resolve("src/main/kotlin/$pkg/Pricing.kt")
        target.parentFile.mkdirs()
        val code = normalizeCode(extractCode(content), pkg.replace('/', '.'))
        target.writeText(code + "\n")
        return listOf("src/main/kotlin/$pkg/Pricing.kt (${code.lines().size} lines)")
    }

    /** See [implementationBuildScript] — shared with the interactive path. */
    private fun implFromSpecBuildScript(): String = implementationBuildScript(
        types = prop("implCoreSrc") ?: DEFAULT_IMPL_CORE_SRC,
        tests = prop("implTests") ?: DEFAULT_IMPL_TESTS,
        probe = prop("implProbe") ?: DEFAULT_IMPL_PROBE,
    )


    /**
     * Writes the verdict lines, named after the spec they are about so repeats can be compared.
     *
     * The **parent directory is part of the name**, and has to be: the fixture corpus holds
     * `written/claude-code.md` and `compressed/claude-code.md`, and on the basename alone the second
     * extraction silently overwrote the first. `scoreExtraction` then read one file as the answer to
     * two different key entries and reported a clean 18/20 for a run that had only covered one spec.
     */
    private fun writeExtraction(outDir: File, content: String, specPath: String): List<String> {
        val lines = content.lines().map { it.trim() }.filter { Regex("^\\d+\\s*:").containsMatchIn(it) }
        val parts = specPath.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
        val relative = parts.takeLast(2).joinToString("/")
        val target = outDir.resolve("$relative.txt")
        target.parentFile.mkdirs()
        target.writeText(lines.joinToString("\n") + "\n")
        return listOf("$relative.txt (${lines.size} verdicts)")
    }

    /**
     * The compression prompt (11.4b): several specs of the same feature, deliberately ANONYMISED as
     * A/B/C so nothing defers to "the good one", and the task of producing one that keeps everything
     * that changes an implementation and nothing else. `-PspecsFrom=<dir>` selects the corpus.
     */
    private fun buildCompressPrompt(root: File): String {
        val dir = root.resolve(prop("specsFrom") ?: "specs")
        val found = dir.listFiles()?.sortedBy { it.name }?.mapNotNull { d ->
            d.resolve("SPEC.md").takeIf { it.isFile }?.readText()
        }.orEmpty()
        require(found.size >= 2) { "Need at least two specs under $dir to compress." }
        return buildString {
            appendLine("Several specifications of the same feature, written independently.")
            appendLine()
            found.forEachIndexed { i, text ->
                appendLine("===== SPEC ${'A' + i} =====")
                appendLine(text)
                appendLine()
            }
            append("Produce the merged specification.")
        }
    }

    /** Writes the reply as `SPEC.md`, unwrapping one enclosing markdown fence if the model added one. */
    private fun writeSpec(outDir: File, content: String): List<String> {
        val whole = Regex("^\\s*```(?:markdown|md)?\\s*\\n(.*)```\\s*$", RegexOption.DOT_MATCHES_ALL)
            .find(content)?.groupValues?.get(1)
        val text = (whole ?: content).trim()
        val target = outDir.resolve("SPEC.md")
        target.parentFile.mkdirs()
        target.writeText(text + "\n")
        return listOf("SPEC.md (${text.lines().size} lines)")
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
        /** Shared types the agent is shown verbatim. NOT DiscountRule — see buildImplFromSpecPrompt. */
        const val DEFAULT_IMPL_TYPES =
            "../spec-test-driven/core/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/admission/Domain.kt"

        /** The real sealed type, read only to count how many cases the brief leaves unshown. */
        const val DEFAULT_IMPL_RULE_TYPE =
            "../spec-test-driven/core/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/DiscountRule.kt"

        /** The brief the specification was written against; the rule surface is read from it. */
        const val DEFAULT_IMPL_SURFACE = "../spec-test-driven/exercises/write-spec/README.md"
        const val DEFAULT_IMPL_CORE_SRC = "../spec-test-driven/core/src/main/kotlin"
        const val DEFAULT_IMPL_TESTS = "pricing-properties/kotlin"

        /** The differential probe, recorded alongside the properties. */
        const val DEFAULT_IMPL_PROBE = "../spec-test-driven/tools/pricing-probe/kotlin"
        const val DEFAULT_IMPL_PACKAGE = "org/jetbrains/kotlin/course/duck/shop/pricing"

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
