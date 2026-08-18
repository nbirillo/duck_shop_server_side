// Records what the REFERENCE `quote` answers on the 11.5 probe corpus. Teacher-only.
//
// A module of its own rather than another source dir on :reference-probe, because both probes write to
// the same probe.out and would overwrite each other's recording.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

    sourceSets.named("main") {
        kotlin.srcDir(rootDir.resolve("reference/src/main/kotlin"))
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
    systemProperty("probe.out", rootDir.resolve("reference-probe-quote/recording.txt").absolutePath)
}
