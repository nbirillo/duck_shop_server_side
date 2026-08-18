// What you inherited: the franchise implementation someone already had an agent write, and the tests
// that came with it. Both are yours to change or throw away.
//
// `ignoreFailures` is NOT set here. If a test you inherited goes red, that is a thing to look at.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    sourceSets.named("main") {
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/core/src/main/kotlin"))
    }
}

repositories { mavenCentral() }

dependencies { testImplementation(kotlin("test")) }

tasks.test { useJUnitPlatform() }
