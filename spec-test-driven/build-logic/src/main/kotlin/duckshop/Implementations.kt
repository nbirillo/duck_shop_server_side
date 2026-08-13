package duckshop

/**
 * The build script of one implement-from-spec module (11.4, step 3e), shared by the API path
 * (`runAgent -Pmode=impl-from-spec`) and the interactive path (`prepareImplementation`).
 *
 * The module compiles ONE agent's implementation, written from a specification alone, against the
 * same shared types the reference uses, and runs the property catalog over it.
 *
 * `ignoreFailures` is on because a failing property here is a **finding, not a build error**: the
 * report has to reach the point where it can name the divergence, and a divergence may well be
 * legitimate — the specification decided something differently from our reference, or left it open.
 */
internal fun implementationBuildScript(types: String, tests: String, probe: String = ""): String {
    // Both optional, and `tests` is empty in the STUDENT build on purpose: the property catalog
    // states the claims, so handing it to a learner hands over what the reference decided. A learner
    // records the probe and compares two readers; the properties stay on the teacher's side.
    val probeSrc = if (probe.isEmpty()) "" else "kotlin.srcDir(rootDir.resolve(\"$probe\"))"
    val testsSrc = if (tests.isEmpty()) "" else "kotlin.srcDir(rootDir.resolve(\"$tests\"))"
    return """
    // GENERATED — do not edit by hand.
    //
    // One agent's implementation of priceFor, written from a specification and nothing else, with
    // the property catalog as its test suite. A red property is a divergence to be read, not a
    // failure to be fixed.

    plugins {
        kotlin("jvm")
    }

    kotlin {
        jvmToolchain(21)

        sourceSets.named("main") {
            kotlin.srcDir(rootDir.resolve("$types"))
        }
        sourceSets.named("test") {
            $testsSrc
            $probeSrc
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
        ignoreFailures = true
        // Each module records into its own folder, so two implementations of one specification can
        // be diffed without either overwriting the other.
        systemProperty("probe.out", layout.buildDirectory.file("probe/recording.txt").get().asFile.absolutePath)
    }

    """.trimIndent()
}
