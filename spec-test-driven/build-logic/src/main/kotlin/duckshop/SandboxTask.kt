package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * Lays out a folder for the implement-from-spec step (11.4) containing the data types, the
 * learner's specification, and nothing else. The learner then opens their agent **in that folder**
 * and works the way they always do — files, iteration, compiling — with no special protocol.
 *
 * ### Why this exists rather than a curated prompt
 *
 * The first version of this step pasted a hand-assembled prompt into a chat window with no file
 * access. That measures a situation which never occurs: a learner opens an agent inside the project
 * and says "implement this from my spec". Measuring the sterile case answers a question nobody asked.
 *
 * ### What it can and cannot guarantee
 *
 * The reference and the property catalog are in a different build, so they are genuinely out of
 * reach. **The brief is not.** It ships with the exercise, and a learner who has the repository has
 * it; no directory layout changes that, and neither would a temporary folder outside the checkout,
 * because an agent can walk upwards.
 *
 * So this is honest about what it is: the sandbox makes the correct path the **default** one, and
 * the exercise says once, plainly, that pointing the agent at the brief means the run measures the
 * brief instead of your specification — which costs you the only feedback the step provides. Same
 * stance as revealing the reference: available, discouraged, and explained rather than policed.
 *
 * Params: `-Pspec=<SPEC.md>`, `[-Pname=<folder>]`, `[-Ptier=basic|advanced]`.
 */
abstract class SandboxTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val specPath = providers.gradleProperty("spec").orNull
            ?: error("Missing -Pspec=<your SPEC.md>")
        val spec = root.resolve(specPath)
        require(spec.isFile) { "Specification not found: $spec" }

        val name = providers.gradleProperty("name").orNull
            ?: specPath.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
                .takeLast(2).joinToString("-")
        val dir = root.resolve("sandbox/$name")
        val pricing = dir.resolve("src/main/kotlin/$PKG/pricing/Pricing.kt")
        val force = providers.gradleProperty("force").isPresent

        // Refusing to clobber, because the previous version did. This folder is gitignored — it is
        // derived — so an agent's work here has no other copy, and re-running with the same -Pname
        // used to delete it without a word.
        if (pricing.isFile && pricing.readText() != stub() && !force) {
            error(
                "sandbox/$name already holds an implementation. Nothing here is under version " +
                    "control, so this task will not delete it.\n" +
                    "  keep it   — use a different -Pname for the next attempt\n" +
                    "  drop it   — add -Pforce, or delete sandbox/$name yourself",
            )
        }

        // The copy of the specification is the one an agent reads, and it is the one a learner is
        // looking at while they talk to it — so edits land in the wrong file and vanish on the next
        // run. Say so rather than discovering it later.
        // An agent that cannot satisfy a property sometimes edits the property. In 11.2 that was the
        // whole attack surface; here it is silent, because the copy is derived and nobody looks at it.
        val editedProperties = (dir.resolve("src/test/kotlin").takeIf { it.isDirectory }
            ?.walkTopDown()?.filter { it.extension == "kt" }?.filter { copy ->
                val origin = root.resolve(
                    providers.gradleProperty("properties").getOrElse("exercises/write-spec/properties"),
                ).resolve(copy.relativeTo(dir.resolve("src/test/kotlin")).path)
                !origin.isFile || origin.readText() != copy.readText()
            }?.toList().orEmpty())
        if (editedProperties.isNotEmpty()) {
            logger.warn("")
            logger.warn("[sandbox] The properties in sandbox/$name were CHANGED:")
            editedProperties.forEach { logger.warn("[sandbox]   ${it.relativeTo(dir)}") }
            logger.warn("[sandbox] If the agent did that, read it before anything else — a property")
            logger.warn("[sandbox] edited to pass is the oldest trick there is, and it is exactly what")
            logger.warn("[sandbox] exercise 11.2 was about. Your originals are untouched.")
        }

        val copied = dir.resolve("SPEC.md")
        if (copied.isFile && copied.readText() != spec.readText()) {
            logger.warn("")
            logger.warn("[sandbox] sandbox/$name/SPEC.md differs from ${spec.relativeTo(root)}.")
            logger.warn("[sandbox] The copy is DERIVED and about to be overwritten. If those edits")
            logger.warn("[sandbox] were yours, put them in ${spec.relativeTo(root)} — that is the file")
            logger.warn("[sandbox] you are graded on, and the only one under version control.")
        }

        dir.deleteRecursively()

        val pkg = PKG
        val src = dir.resolve("src/main/kotlin/$pkg")
        src.resolve("admission").mkdirs()
        src.resolve("pricing").mkdirs()

        val core = root.resolve("core/src/main/kotlin/$pkg")
        src.resolve("admission/Domain.kt").writeText(core.resolve("admission/Domain.kt").readText())
        // AdmissionPolicy comes along because OnlyIf refers to it; the leaves do not, so the
        // sandbox never becomes a second copy of exercise 11.2.
        src.resolve("admission/AdmissionPolicy.kt")
            .writeText(core.resolve("admission/AdmissionPolicy.kt").readText())
        src.resolve("pricing/DiscountRule.kt")
            .writeText(withoutExerciseNotes(core.resolve("pricing/DiscountRule.kt").readText()))
        src.resolve("pricing/Pricing.kt").writeText(stub())

        // The learner's OWN properties, if they wrote any. Ours never come here — they state the
        // claims, so they would hand over what the reference decided — but a learner's own are theirs,
        // and putting them in front of the agent is the point: the agent now has something to satisfy
        // rather than only prose to interpret. That is the 11.2 loop, driven by their specification.
        val ownProperties = root.resolve(
            providers.gradleProperty("properties").getOrElse("exercises/write-spec/properties"),
        )
        val propertyFiles = ownProperties.takeIf { it.isDirectory }
            ?.walkTopDown()?.filter { it.extension == "kt" }?.toList().orEmpty()
        propertyFiles.forEach { file ->
            val target = dir.resolve("src/test/kotlin").resolve(file.relativeTo(ownProperties).path)
            target.parentFile.mkdirs()
            file.copyTo(target, overwrite = true)
        }

        dir.resolve("SPEC.md").writeText(spec.readText())
        dir.resolve("README.md").writeText(readme(name))
        dir.resolve("build.gradle.kts").writeText(buildScript(propertyFiles.isNotEmpty()))
        // Its OWN settings file, or Gradle walks up and adopts the student build's — and then the
        // sandbox is a module of the very project it is supposed to be separate from.
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"$name\"\n")

        logger.lifecycle("")
        logger.lifecycle("sandbox/$name is ready. It holds the data types, your SPEC.md, and a stub.")
        logger.lifecycle("")
        logger.lifecycle("Open your agent IN THAT FOLDER and ask it to implement priceFor from SPEC.md.")
        logger.lifecycle("Do not point it at the brief: then it answers from the brief, and the step")
        logger.lifecycle("stops telling you anything about your specification.")
        logger.lifecycle("")
        logger.lifecycle("  ./gradlew -p sandbox/$name compileKotlin     # does it build")
    }

    /**
     * Strips the KDoc from `DiscountRule.kt`. It says in as many words that defining this behaviour
     * *is* exercise 11.4 and points at the tier structure — a straight answer key for any agent that
     * opens the file. Declarations only.
     */
    private fun withoutExerciseNotes(source: String): String =
        source.replace(Regex("""/\*\*[\s\S]*?\*/\s*""", RegexOption.MULTILINE), "")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim() + "\n"

    private fun stub(): String =
        """
        package org.jetbrains.kotlin.course.duck.shop.pricing

        import org.jetbrains.kotlin.course.duck.shop.admission.Duck

        // Implement these from SPEC.md. Where the specification does not settle something, decide
        // and move on — a gap is not a reason to stop, and what you decide is exactly what the
        // exercise is looking at.
        //
        // DiscountRule may have cases SPEC.md never describes. Leave the price unchanged for those:
        // your `when` still has to be exhaustive.
        //
        // If src/test/ holds properties, they are the contract in executable form: make them pass
        // WITHOUT editing them. A property you cannot satisfy is a conversation, not an obstacle.

        fun priceFor(duck: Duck, rules: List<DiscountRule>): Int = TODO()

        """.trimIndent()

    private fun readme(name: String): String =
        """
        # Implement from the specification

        **`SPEC.md` here is a copy.** The original lives in `exercises/write-spec/SPEC.md`, it is the
        file you are graded on, and it is the only one under version control. This whole folder is
        derived and ignored by git — if you decide to change your specification, change the original.

        This folder has the data types, that copy, and a stub in
        `src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/Pricing.kt`.

        Open your agent here and ask it to implement `priceFor` from `SPEC.md`. Work however you
        normally would.

        **One rule.** Do not give it the original business brief, and do not paste the rules in
        yourself. The whole point of this step is to find out what your *specification* carries. Feed
        it the brief and you will get working code and learn nothing — the agent will be answering
        the brief's question, not yours.

        Nothing stops you. The brief is in the repository you downloaded and an agent can find it if
        you send it looking. This is the same deal as the reference implementation: available,
        and it costs you the exercise.

        From the project root:

            ./gradlew -p sandbox/$name compileKotlin   # does it build
            ./gradlew -p sandbox/$name test            # if you brought properties of your own

        If `src/test/` has properties in it, they came from `exercises/write-spec/properties/` and they
        are your contract in executable form. **The agent must make them pass without editing them.**
        A property edited to pass is the oldest trick there is — it is what exercise 11.2 was about —
        and this task tells you when the copies stop matching your originals.

        Whether the implementation is *right* is not something this folder can settle. That comes from
        the checks your teacher runs, and from reading the divergences with your specification open
        beside them.

        When it compiles, hand it back:

            ./gradlew prepareImplementation -Pagent=<you> -Pspec=<the spec> -Pfrom=sandbox/$name
        """.trimIndent() + "\n"

    /**
     * The sandbox build. It grows a test setup only when the learner brought properties of their own —
     * with none, there is deliberately nothing to run here, so the step cannot turn into "make these
     * pass" against a suite somebody else wrote.
     */
    private fun buildScript(hasProperties: Boolean): String = BUILD_SCRIPT + if (!hasProperties) "" else
        """

        dependencies {
            testImplementation(kotlin("test"))
        }

        tasks.test {
            useJUnitPlatform()
        }
        """.trimIndent()

    private companion object {
        const val PKG = "org/jetbrains/kotlin/course/duck/shop"

        val BUILD_SCRIPT = """
            // GENERATED by `./gradlew sandbox` — do not edit by hand.
            //
            // Compiles the types and your implementation, and nothing else. There is deliberately no
            // test suite here: the properties your specification will be checked against are part of
            // the checking, and having them in the folder would turn the step into "make these pass".

            plugins {
                kotlin("jvm") version "2.2.20"
            }

            kotlin {
                jvmToolchain(21)
            }

            repositories {
                mavenCentral()
            }

        """.trimIndent()
    }
}
