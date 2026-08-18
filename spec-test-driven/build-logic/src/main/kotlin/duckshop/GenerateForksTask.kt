package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Turns a fork catalog into one Gradle module per reading. See [Forks] for what a reading is and why
 * the capstone is graded on readings rather than on hidden corner cases.
 *
 * Every generated module is self-contained: `:core`'s types, the reading's own `bestOffer`, and a
 * pricing implementation, all compiled together with the suite under test.
 */
abstract class GenerateForksTask @Inject constructor(
    private val layout: ProjectLayout,
) : DefaultTask() {

    @get:Input
    abstract val catalogPath: Property<String>

    @get:Input
    abstract val outPath: Property<String>

    @get:Input
    abstract val coreSrc: Property<String>

    /**
     * The `priceFor` implementation each reading needs, copied in as `Pricing.kt`.
     *
     * A property rather than a fixed path, and it matters which one each build points at: the teacher
     * build uses the reference, while **the student build must use the learner's own inherited
     * implementation.** Putting a correct pricing file anywhere inside the student folder would hand
     * over the answer to exercise 11.4, which is the leak [module-11 reference-impl leakage] exists to
     * prevent.
     */
    @get:Input
    abstract val pricingSrc: Property<String>

    /** The test source directory to run against every reading. */
    @get:Input
    abstract val testsSrc: Property<String>

    @get:Input
    abstract val packagePath: Property<String>

    @TaskAction
    fun generate() {
        val root = layout.projectDirectory.asFile
        val catalog = root.resolve(catalogPath.get())
        val readings = loadForkCatalog(catalog)
        val outDir = root.resolve(outPath.get())
        val pkg = packagePath.get()

        val pricing = root.resolve(pricingSrc.get())
        require(pricing.isFile) {
            "No pricing implementation at $pricing (set -PforksPricing=<file>). Every reading needs a " +
                "priceFor to build a price with; in the student build that is the implementation the " +
                "capstone inherits, never a copy of the reference."
        }

        readings.forEach { reading ->
            val moduleDir = outDir.resolve(reading.id)
            val source = catalog.parentFile.resolve(reading.source)
            require(source.isFile) { "Reading '${reading.id}' points at a missing source: $source" }

            // Wipe first: a renamed reading source would otherwise leave the previous one behind and
            // the module would compile two `bestOffer`s, or worse, keep scoring a stale reading.
            moduleDir.resolve("src").deleteRecursively()
            val mainDir = moduleDir.resolve("src/main/kotlin/$pkg")
            mainDir.mkdirs()
            source.copyTo(mainDir.resolve("BestOffer.kt"), overwrite = true)
            pricing.copyTo(mainDir.resolve("Pricing.kt"), overwrite = true)

            moduleDir.resolve("build.gradle.kts")
                .writeText(forkBuildScript(coreSrc = coreSrc.get(), testsSrc = testsSrc.get()))
        }

        // The suite is BAKED INTO the generated build scripts, so the report has no way to know which
        // one actually ran. Recording it is the guard against this repo's most persistent fault: a
        // build-level property standing in for a per-module fact, which has already made three reports
        // name a suite that never ran (mutantTests, mutantsCoreSrc, specSurface).
        outDir.resolve("generated-with.txt").writeText(
            "tests=${testsSrc.get()}\npricing=${pricingSrc.get()}\ncore=${coreSrc.get()}\n",
        )

        val byFork = readings.groupBy { it.fork }
        logger.lifecycle(
            "[generateForks] ${readings.size} readings of ${byFork.size} forks under " +
                "${outDir.relativeTo(root)}: " + byFork.entries.joinToString { "${it.key}×${it.value.size}" },
        )
        logger.lifecycle("[generateForks] reload Gradle, then: ./gradlew verifyForks")
    }
}
