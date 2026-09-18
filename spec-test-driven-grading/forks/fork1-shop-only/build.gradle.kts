// GENERATED — do not edit by hand. Edit the catalog and re-run generateForks.
//
// One READING of a fork the brief leaves open, compiled with the suite under test. A failing test
// means the suite rejects this reading, which is how a decision becomes visible.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

    sourceSets.named("main") {
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/core/src/main/kotlin"))
    }
    sourceSets.named("test") {
        kotlin.srcDir(rootDir.resolve("tests-11.6/kotlin"))
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
}

// A compile failure must leave NO results behind. `test` never starts when compilation fails, so
// the previous run's XML would still be sitting there and the report would print that score for a
// suite that no longer builds — measured: a clean "1/4 killed" for a suite with a type error in it.
// Clearing as compilation STARTS is what makes the report's "did not compile" branch mean it.
val resultsDir = layout.buildDirectory.dir("test-results/test")
listOf("compileKotlin", "compileTestKotlin").forEach { stage ->
    tasks.named(stage) { doFirst { resultsDir.get().asFile.deleteRecursively() } }
}
