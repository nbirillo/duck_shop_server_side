// Records what the REFERENCE `bestOffer` answers on the 11.6 probe corpus. Teacher-only.

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
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/tools/franchise-probe/kotlin"))
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
    systemProperty("probe.out", rootDir.resolve("reference-probe-franchise/recording.txt").absolutePath)
}
