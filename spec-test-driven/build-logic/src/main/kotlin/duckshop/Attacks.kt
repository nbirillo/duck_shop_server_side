package duckshop

import java.io.File

/**
 * Assembling an attack module, shared by the two ways one arrives: `runAgent -Pmode=attack` for an
 * API agent, and `prepareAttack` for an interactive agent that wrote the files itself.
 */

/** Names of the top-level types a Kotlin source declares — enough to tell two files apart. */
internal fun declarations(code: String): Set<String> =
    Regex(
        "^(?:@\\w+\\s+)*(?:public |internal |private )?(?:abstract |open |sealed |data |value |fun )*" +
            "(?:class|interface|object)\\s+(\\w+)",
        RegexOption.MULTILINE,
    ).findAll(code).map { it.groupValues[1] }.toSet()

/**
 * Renames anything under the attack's `src/main` that collides with a `:core` file name.
 *
 * The module excludes the `:core` file it replaces, and that exclude applies to every source dir in
 * the set — so a replacement stored as `Leaves.kt` would be excluded along with the original and its
 * classes would simply vanish. `LeavesAttack.kt` keeps both working, the same way the mutant
 * generator writes `LeavesMutated.kt`.
 */
internal fun renameCollisions(coreBase: File, attackBase: File, log: (String) -> Unit) {
    attackBase.walkTopDown()
        .filter { it.isFile && it.extension == "kt" && coreBase.resolve(it.name).isFile }
        .toList()
        .forEach { file ->
            val renamed = file.resolveSibling(file.name.removeSuffix(".kt") + "Attack.kt")
            file.renameTo(renamed)
            log("renamed ${file.name} to ${renamed.name} so the exclude of the original still works")
        }
}

/**
 * Build script for an attack module: the real `:core` sources with the rewritten files left out and
 * the attacking versions compiled in their place — the same shape a mutant module has, except the
 * change came from an agent rather than from a catalog. It also compiles the differential probe, so
 * `verifyAttack` can compare its answers with the reference module's.
 */
internal fun attackBuildScript(
    coreBase: File,
    attackBase: File,
    defaultSuite: String,
    warn: (String) -> Unit,
): String {
    val declaredByAttack = attackBase.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .flatMap { declarations(it.readText()) }
        .toSet()

    // Which :core files the attack has taken over. Deciding this by the class names it declares
    // rather than by the file name it claimed keeps weak models in the measurement: they routinely
    // drop the `// FILE:` marker, and inferring the target is wrapping, not logic. A file counts as
    // replaced only if EVERY declaration in it was rewritten — excluding one whose other classes
    // nobody supplied would simply delete them.
    val coreFiles = coreBase.listFiles { f: File -> f.isFile && f.extension == "kt" }.orEmpty().sortedBy { it.name }
    val replaced = coreFiles
        .filter { file ->
            val declared = declarations(file.readText())
            declared.isNotEmpty() && declaredByAttack.containsAll(declared)
        }
        .map { it.name }

    val covered = replaced.flatMap { declarations(coreBase.resolve(it).readText()) }.toSet()
    if (replaced.isEmpty() || (declaredByAttack - covered).isNotEmpty()) {
        warn(
            "the attack declares ${declaredByAttack.sorted()} but only fully replaces " +
                "${replaced.ifEmpty { "nothing" }}. Declarations that clash with a :core file still " +
                "compiled in will not build — inspect src/main before scoring.",
        )
    }
    val excludes = replaced.joinToString("\n        ") { """kotlin.exclude("**/$it")""" }

    return """
        // GENERATED — do not edit by hand; regenerate with `./gradlew prepareAttack -Pagent=<name>`.
        //
        // Compiles the real :core sources with ${replaced.joinToString(", ").ifEmpty { "nothing" }}
        // replaced by the attacking implementation under src/main, then runs the suite it was aimed
        // at. The attack succeeds only if that suite stays GREEN while the probe shows the behaviour
        // really did change.

        plugins {
            kotlin("jvm")
        }

        kotlin {
            jvmToolchain(21)

            sourceSets.named("main") {
                kotlin.srcDir(rootDir.resolve("core/src/main/kotlin"))
                $excludes
            }
            sourceSets.named("test") {
                // Same selector the mutant modules use, so one -PmutantTests drives the whole check.
                // The suite this attack was aimed at is recorded in agent.json.
                kotlin.srcDir(
                    rootDir.resolve(
                        when (val selected = providers.gradleProperty("mutantTests").getOrElse("learner")) {
                            "learner" -> "$defaultSuite"
                            else -> selected
                        },
                    ),
                )
                kotlin.srcDir(rootDir.resolve("tools/probe/kotlin"))
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
            // verifyAttack reads the XML results, so a suite that catches the attack must not break
            // the build before the report is written.
            ignoreFailures = true
        }

        // A compile failure must leave NO results behind. `test` never starts when compilation fails, so
        // the previous run's XML would still be sitting there and the report would print that score for a
        // suite that no longer builds — measured: a clean "1/4 killed" for a suite with a type error in it.
        // Clearing as compilation STARTS is what makes the report's "did not compile" branch mean it.
        val resultsDir = layout.buildDirectory.dir("test-results/test")
        listOf("compileKotlin", "compileTestKotlin").forEach { stage ->
            tasks.named(stage) { doFirst { resultsDir.get().asFile.deleteRecursively() } }
        }

    """.trimIndent()
}
