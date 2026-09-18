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
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/core/src/main/kotlin"))
    }
    sourceSets.named("test") {
        kotlin.srcDir(rootDir.resolve("pricing-properties/kotlin"))
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/tools/pricing-probe/kotlin"))
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

// A compile failure must leave NO results behind. `test` never starts when compilation fails, so
// the previous run's XML would still be sitting there and the report would print that score for a
// suite that no longer builds — measured: a clean "1/4 killed" for a suite with a type error in it.
// Clearing as compilation STARTS is what makes the report's "did not compile" branch mean it.
val resultsDir = layout.buildDirectory.dir("test-results/test")
listOf("compileKotlin", "compileTestKotlin").forEach { stage ->
    tasks.named(stage) { doFirst { resultsDir.get().asFile.deleteRecursively() } }
}
