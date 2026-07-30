// Practice target for exercise 11.2: a self-contained BROKEN algebra. The learner's own tests
// (from exercises/write-tests) are compiled and run against it; a good suite makes some test FAIL
// here (the bug is CAUGHT). ignoreFailures lets `practiceCatch` read the result instead of
// breaking the build.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    sourceSets.named("test") {
        kotlin.srcDir(rootDir.resolve("exercises/write-tests/src/test/kotlin"))
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
