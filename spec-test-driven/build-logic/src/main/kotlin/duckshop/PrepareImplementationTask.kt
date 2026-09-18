package duckshop

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

/**
 * Turns a hand-placed `Pricing.kt` into a module `verifyImplementations` can score.
 *
 * `runAgent -Pmode=impl-from-spec` does this for an API agent. An interactive one — Claude Code,
 * Junie — is driven by a person in another window and has no API call to hang the scaffolding off,
 * so this supplies the same treatment. Same pattern as `prepareAttack`.
 *
 * It also **prints the exact prompt to hand that agent**, assembled from the same sources the API
 * path uses, so the two routes stay comparable. Copying the specification by hand is how a brief or
 * a reference leaks in, and then the run measures the leak instead of the specification.
 *
 * Params: `-Pagent=<name>`, `-Pspec=<the specification>`, `[-PimplSurface=<brief>]`.
 */
abstract class PrepareImplementationTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val agent: Property<String>

    /**
     * The command to print once the module is ready, with `<agent>` standing for the run's name.
     *
     * Named by the build that owns the report. The convention is `verifyDivergence`, which is safe
     * because the plugin registering THIS task registers that one too; the grading build overrides it
     * with `verifyImplementations`, which exists only there. It used to print `verifyImplementations`
     * unconditionally, so in the student build it named a task that is not there.
     */
    @get:Input
    abstract val reportCommand: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val name = agent.orNull ?: providers.gradleProperty("agent").orNull
            ?: error("Missing -Pagent=<name>")
        val spec = providers.gradleProperty("spec").orNull
            ?: error("Missing -Pspec=<the specification this implementation was written from>")
        val key = spec.trimEnd('/').removeSuffix(".md").split('/').filter { it.isNotEmpty() }
            .takeLast(2).joinToString("-")

        val pkg = providers.gradleProperty("implPackage").getOrElse(IMPL_PACKAGE)
        val moduleDir = root.resolve("implementations/$name/$key")
        val source = moduleDir.resolve("src/main/kotlin/$pkg/Pricing.kt")

        // -Pfrom=<sandbox dir> collects the file the agent wrote where it worked, so nobody has to
        // copy it by hand into a path five directories deep and get it subtly wrong.
        providers.gradleProperty("from").orNull?.let { from ->
            val written = root.resolve(from).resolve("src/main/kotlin/$pkg/Pricing.kt")
            require(written.isFile) { "Nothing at $written — has the agent written it yet?" }
            source.parentFile.mkdirs()
            written.copyTo(source, overwrite = true)
            logger.lifecycle("[prepareImplementation] took ${written.relativeTo(root)}")
        }

        if (!source.isFile) {
            logger.lifecycle("")
            logger.lifecycle("Nothing to prepare yet. Put the agent's file here:")
            logger.lifecycle("   ${source.relativeTo(root)}")
            logger.lifecycle("")
            logger.lifecycle("Hand the agent this and nothing else — no brief, no reference, no properties:")
            logger.lifecycle("")
            logger.lifecycle("─".repeat(78))
            logger.lifecycle(promptFor(root, spec))
            logger.lifecycle("─".repeat(78))
            return
        }

        moduleDir.resolve("build.gradle.kts").writeText(
            implementationBuildScript(
                types = providers.gradleProperty("implCoreSrc").getOrElse(IMPL_CORE_SRC),
                tests = providers.gradleProperty("implTests").getOrElse(IMPL_TESTS),
                probe = providers.gradleProperty("implProbe").getOrElse(IMPL_PROBE),
            ),
        )
        moduleDir.resolve("agent.json").writeText(
            buildJsonObject {
                put("agent", name)
                put("mode", "impl-from-spec")
                put("provider", "interactive")
                put("spec", spec)
                put("checkedOn", LocalDate.now().toString())
            }.toString() + "\n",
        )

        logger.lifecycle("[prepareImplementation] implementations/$name/$key is ready — reload Gradle, then:")
        logger.lifecycle("[prepareImplementation]   ${reportCommand.get().replace("<agent>", name)}")
    }

    /** The API path's prompt, printed so an interactive run gets exactly the same input. */
    private fun promptFor(root: File, spec: String): String {
        val system = root.resolve(
            providers.gradleProperty("promptDir").getOrElse("tools") + "/agent-prompt-impl-spec.md",
        )
        require(system.isFile) { "Prompt not found: $system (pass -PpromptDir=<dir>)" }
        return system.readText().trim() + "\n\n" +
            "Then paste, below this line, the contents of $spec and of the surface block from " +
            providers.gradleProperty("implSurface").getOrElse(IMPL_SURFACE) +
            ".\nRun `./gradlew runAgent -Pmode=impl-from-spec -Pdry ...` with the same -Pspec to " +
            "print the exact\nuser message the API path would send, and use that verbatim."
    }

    // Defaults for the STUDENT build, which is the one a learner runs. The grading build overrides
    // every one of them in its gradle.properties — including implTests, which is empty here on
    // purpose: the property catalog states the claims, and a learner must not have it.
    private companion object {
        const val IMPL_PACKAGE = "org/jetbrains/kotlin/course/duck/shop/pricing"
        const val IMPL_CORE_SRC = "core/src/main/kotlin"
        const val IMPL_TESTS = ""
        const val IMPL_PROBE = "tools/pricing-probe/kotlin"
        const val IMPL_SURFACE = "exercises/write-spec/README.md"
    }
}
