package duckshop

import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

/**
 * Generates the mutant modules for mutation testing from a catalog.
 *
 * Each catalog entry becomes a module under the output dir that compiles the REAL `:core` sources
 * with one file replaced by a mutated copy, and runs a test suite against the result. Because the
 * unchanged sources come straight from `:core`, mutants never drift from the algebra and every
 * `:core` type (including `Shop`) is available to the suite.
 *
 * The generated modules are committed on purpose: a learner can open a mutant and read the injected
 * defect, and the exported student folder works without a generation step. Regenerate after editing
 * the catalog (or `:core`) and review the diff.
 *
 * Paths are taken from gradle properties so the same task serves the student build and the
 * teacher-only grading build: `mutantsCatalog`, `mutantsCoreSrc`, `mutantsLearnerTests`,
 * `mutantsOut`.
 */
abstract class GenerateMutantsTask @Inject constructor(
    private val providers: ProviderFactory,
    private val layout: ProjectLayout,
) : DefaultTask() {

    /** Catalog to generate from, relative to this build's root. */
    @get:Input
    abstract val catalogPath: Property<String>

    /** Where the generated modules go, relative to this build's root. */
    @get:Input
    abstract val outPath: Property<String>

    /**
     * Source dirs compiled **unmutated** alongside the mutated tree, comma-separated.
     *
     * The admission mutants do not need this: the file being mutated lives in `:core`, so compiling
     * `:core` is the whole story. The pricing reference does — it lives in the grading build, outside
     * `:core`, yet still needs `Duck`, `DiscountRule` and `AdmissionPolicy` to compile against.
     */
    @get:Input
    abstract val alsoCompile: Property<String>

    /** Package dir the mutated file sits in, relative to the source root. */
    @get:Input
    abstract val packagePath: Property<String>

    /**
     * The suite these modules run when `-PmutantTests` is not given.
     *
     * `mutantTests` is a gradle property and therefore global to the build, so a build holding two
     * catalogs cannot express "this one runs the property suite" through it. It has to be baked in
     * per catalog at generation time. The 11.4 pricing mutants defaulted to the 11.2 admission suite
     * and ran it — ten tests about policies, against mutated pricing code, one of them failing in
     * the baseline too. Nothing about that reads as a misconfiguration in the output.
     */
    @get:Input
    abstract val defaultTests: Property<String>

    /**
     * Source root the mutated file is read from, relative to this build's root. Same reasoning as
     * [defaultTests]: `mutantsCoreSrc` is global to the build, and this build mutates `:core` for
     * 11.2 and the pricing reference for 11.4. Falls back to the gradle property when unset.
     */
    @get:Input
    @get:Optional
    abstract val coreSrc: Property<String>

    @TaskAction
    fun run() {
        val root = layout.projectDirectory.asFile
        val catalogPath = catalogPath.get().trim('/')
        val outPath = outPath.get().trim('/')
        val coreSrc = (coreSrc.orNull ?: pathProp("mutantsCoreSrc", "core/src/main/kotlin")).trim('/')
        val pkg = packagePath.getOrElse(ADMISSION_PACKAGE_PATH).trim('/')
        val extra = alsoCompile.getOrElse("").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val learnerTests = pathProp("mutantsLearnerTests", "exercises/write-tests/src/test/kotlin")
        val defaults = defaultTests.getOrElse("learner")

        val catalog = loadCatalog(root.resolve(catalogPath))
        val outDir = root.resolve(outPath)

        catalog.forEach { mutant ->
            val source = root.resolve("$coreSrc/$pkg/${mutant.file}")
            require(source.isFile) { "Mutant '${mutant.id}': source file not found: $source" }

            val original = source.readText()
            // Sanity guard: a pattern that no longer matches (or matches twice) would silently
            // produce a mutant identical to :core — one that no suite can kill, i.e. a false green.
            val hits = original.split(mutant.find).size - 1
            require(hits == 1) {
                "Mutant '${mutant.id}': the pattern must occur EXACTLY once in ${mutant.file}, found $hits. " +
                    "The catalog is out of sync with :core — update \"find\"."
            }
            val mutated = original.replace(mutant.find, mutant.replace)
            require(mutated != original) { "Mutant '${mutant.id}': \"replace\" is identical to \"find\"." }

            val moduleDir = outDir.resolve(mutant.id)
            // Drop previously generated sources so a catalog entry that now mutates another file
            // does not leave a stale copy behind.
            moduleDir.resolve("src").deleteRecursively()

            val subDir = mutant.file.substringBeforeLast('/', "")
            val baseName = mutant.file.substringAfterLast('/').removeSuffix(".kt")
            val targetDir = moduleDir
                .resolve("src/main/kotlin/$pkg")
                .resolve(subDir)
            targetDir.mkdirs()
            targetDir.resolve("${baseName}Mutated.kt").writeText(header(mutant, catalogPath) + mutated)
            moduleDir.resolve("build.gradle.kts")
                .writeText(buildScript(mutant, coreSrc, learnerTests, extra, defaults))

            logger.lifecycle("[$name] ${mutant.id} (${mutant.bucket}) — ${mutant.file} ${mutant.what}".trimEnd())
        }

        // The baseline: the same suite against UNMUTATED :core. A suite that is red here contradicts
        // the correct code, and would "kill" every mutant with that same failure — so the mutation
        // score means nothing until the baseline is green.
        val baselineDir = outDir.resolve(BASELINE)
        baselineDir.mkdirs()
        baselineDir.resolve("build.gradle.kts")
            .writeText(buildScript(null, coreSrc, learnerTests, extra, defaults))

        val stale = (outDir.listFiles() ?: emptyArray())
            .filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
            .map { it.name }
            .filterNot { it == BASELINE }
            .filterNot { name -> catalog.any { it.id == name } }
        if (stale.isNotEmpty()) {
            logger.warn("[$name] folders no longer in the catalog (delete them by hand): $stale")
        }
        logger.lifecycle(
            "[$name] ${catalog.size} module(s) written under $outPath — reload Gradle, then: " +
                "./gradlew ${name.replace("generate", "verify")}",
        )
    }

    private fun pathProp(name: String, default: String): String =
        providers.gradleProperty(name).getOrElse(default).trim('/')

    private fun header(mutant: Mutant, catalogPath: String): String = buildString {
        // A multi-line "replace" would otherwise break out of the comment and land above `package`.
        fun oneLine(s: String) = s.replace("\n", "\\n").replace(Regex("\\s+"), " ").trim()
        appendLine("// GENERATED by `./gradlew $name` from $catalogPath — do not edit by hand.")
        appendLine("//")
        appendLine("// ${if (mutant.conformant) "Variant" else "Mutant"} ${mutant.id} (${mutant.bucket})")
        appendLine("//   ${mutant.file}:  ${oneLine(mutant.find)}  ->  ${oneLine(mutant.replace)}")
        // Prose descriptions exist only in the teacher-only catalog: naming the defect and the test
        // that catches it would answer the exercise for whoever (or whatever) reads this folder.
        if (mutant.what.isNotEmpty()) {
            appendLine("//")
            appendLine("// ${mutant.what}")
        }
        if (mutant.hint.isNotEmpty()) appendLine("// A suite that kills this mutant: ${mutant.hint}")
        appendLine()
    }

    /** The build script of one mutant module, or of the baseline module when [mutant] is null. */
    private fun buildScript(
        mutant: Mutant?,
        coreSrc: String,
        learnerTests: String,
        alsoCompile: List<String> = emptyList(),
        defaults: String = "learner",
    ): String =
        """
        // GENERATED by `./gradlew $name` — do not edit by hand; edit the catalog.
        //
        ${if (mutant != null) "// ${if (mutant.conformant) "Variant" else "Mutant"} ${mutant.id} (${mutant.bucket})${if (mutant.what.isEmpty()) "" else ": ${mutant.what}"}" else "// BASELINE: no mutation at all."}
        //
        ${
            if (mutant != null) {
                "// Compiles the real :core sources with ${mutant.file} REPLACED by the modified copy under\n" +
                    "        // src/main, then runs a test suite against the result. " +
                    if (mutant.conformant) {
                        "This one is a legal refactoring:\n" +
                            "        // behaviour is unchanged, so a faithful suite stays GREEN here."
                    } else {
                        "A good suite KILLS this mutant —\n" +
                            "        // some test fails here."
                    }
            } else {
                "// Runs the selected suite against the UNMUTATED :core, to check the suite is valid in the\n" +
                    "        // first place. A suite that fails here contradicts the correct code, so it would \"kill\"\n" +
                    "        // every mutant with that same failure and its mutation score would be meaningless."
            }
        }

        plugins {
            kotlin("jvm")
        }

        // Which suite runs against the mutant: "learner", or any test source dir given as a path
        // relative to this build's root — set with -PmutantTests=<learner|path>. The default below
        // is baked in per catalog at generation time, because -PmutantTests is global to the build
        // and a build with two catalogs needs two different answers.
        val suite: String = when (val selected = providers.gradleProperty("mutantTests").getOrElse("$defaults")) {
            "learner" -> "$learnerTests"
            else -> selected
        }

        kotlin {
            jvmToolchain(21)

            sourceSets.named("main") {
                kotlin.srcDir(rootDir.resolve("$coreSrc"))
                ${alsoCompile.joinToString("\n                ") { """kotlin.srcDir(rootDir.resolve("$it"))""" }}
                ${if (mutant != null) """kotlin.exclude("**/${mutant.file}")""" else "// no exclude: the baseline compiles the sources as they are"}
            }
            sourceSets.named("test") {
                kotlin.srcDir(rootDir.resolve(suite))
            }
        }

        repositories {
            mavenCentral()
        }

        dependencies {
            testImplementation(kotlin("test"))
        }

        tasks.test {
            useJUnitPlatform()
            // verifyMutants reads the XML results, so a killed mutant must not break the build.
            ignoreFailures = true
        }

        """.trimIndent()
}
