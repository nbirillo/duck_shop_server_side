// Records what the REFERENCE answers on the probe corpus, so implementations written from a
// specification have something to be compared against. Teacher-only, like everything in this build.
//
// It is a hand-written module rather than a generated one because there is exactly one reference and
// it does not change per agent.

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
    systemProperty("probe.out", rootDir.resolve("reference-probe/recording.txt").absolutePath)
}
