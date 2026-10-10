package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * Turns hand-placed sources under `attacks/<agent>/src/main` into a module `verifyAttack` can score.
 *
 * `runAgent -Pmode=attack` does this for an API agent. An interactive agent (Claude Code, Junie)
 * writes the files itself and has no API call to hang the scaffolding off, so this task supplies the
 * same treatment: it renames anything colliding with a `:core` file name and writes the build script
 * with the right excludes worked out from the classes the sources declare.
 */
abstract class PrepareAttackTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Folder name under `attacks/` to prepare. */
    @get:Input
    abstract val agent: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val name = agent.orNull ?: error("Missing -Pagent=<name> (a folder under attacks/)")
        val packagePath = "org/jetbrains/kotlin/course/duck/shop/admission"
        val attackDir = root.resolve("attacks/$name")
        val attackBase = attackDir.resolve("src/main/kotlin/$packagePath")
        require(attackBase.isDirectory) {
            "Put the attacking sources in attacks/$name/src/main/kotlin/$packagePath first."
        }
        val coreBase = root.resolve("core/src/main/kotlin/$packagePath")
        val suite = providers.gradleProperty("mutantTests").orNull
            ?.takeIf { it != "learner" }
            ?: "exercises/write-tests/src/test/kotlin"

        renameCollisions(coreBase, attackBase) { logger.lifecycle("[prepareAttack] $it") }
        attackDir.resolve("build.gradle.kts")
            .writeText(attackBuildScript(coreBase, attackBase, suite) { logger.warn("[prepareAttack] $it") })
        attackDir.resolve("agent.json").writeText(
            buildJsonObject {
                put("agent", name)
                put("mode", "attack")
                put("provider", "interactive")
                put("checkedOn", LocalDate.now().toString())
                put("attackedSuite", suite)
            }.toString() + "\n",
        )

        logger.lifecycle("[prepareAttack] attacks/$name is ready — reload Gradle, then:")
        logger.lifecycle("[prepareAttack]   ./gradlew verifyAttack -Pagent=$name -PmutantTests=$suite --continue")
    }
}
