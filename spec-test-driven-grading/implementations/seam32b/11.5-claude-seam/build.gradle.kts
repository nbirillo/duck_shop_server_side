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
        
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/tools/quote-probe/kotlin"))
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
