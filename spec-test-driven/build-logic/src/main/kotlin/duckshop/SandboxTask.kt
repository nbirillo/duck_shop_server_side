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
        dir.deleteRecursively()

        val pkg = "org/jetbrains/kotlin/course/duck/shop"
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

        dir.resolve("SPEC.md").writeText(spec.readText())
        dir.resolve("README.md").writeText(readme(name))
        dir.resolve("build.gradle.kts").writeText(BUILD_SCRIPT)
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

        fun priceFor(duck: Duck, rules: List<DiscountRule>): Int = TODO()

        """.trimIndent()

    private fun readme(name: String): String =
        """
        # Implement from the specification

        This folder has the data types, your `SPEC.md`, and a stub in
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

        From the project root, `./gradlew -p sandbox/$name compileKotlin` tells you whether it builds. Whether it is *right* is not
        something this folder can tell you — that comes from the checks your teacher runs, and from
        reading the divergences with your specification open beside them.

        When it compiles, hand it back:

            ./gradlew prepareImplementation -Pagent=<you> -Pspec=<the spec> -Pfrom=sandbox/$name
        """.trimIndent() + "\n"

    private companion object {
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
