// Where YOUR tests go. It compiles against what you inherited, so a test you write here runs against
// the implementation currently in place — including one an agent has just rewritten from your spec.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    sourceSets.named("main") {
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/core/src/main/kotlin"))
        kotlin.srcDir(rootDir.resolve("inherited/src/main/kotlin"))
    }
}

repositories { mavenCentral() }

dependencies { testImplementation(kotlin("test")) }

tasks.test { useJUnitPlatform() }
