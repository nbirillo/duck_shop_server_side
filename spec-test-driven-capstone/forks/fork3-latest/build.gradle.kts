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
        kotlin.srcDir(rootDir.resolve("inherited/src/test/kotlin"))
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
