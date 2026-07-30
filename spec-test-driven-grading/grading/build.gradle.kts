// :grading — the teacher-only reference implementation + the shared acceptance suite. Depends on
// :core from the sibling spec-test-driven build (substituted via the composite includeBuild) and
// reuses the single shared test source by relative path. Not the duck-shop.solution convention
// plugin: this build is standalone and must not reach back into the student project's wiring.

plugins {
    kotlin("jvm") version "2.2.20"
}

kotlin {
    jvmToolchain(21)

    // Run the exact same shared acceptance suite the student modules run.
    sourceSets.named("test") {
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/tests/kotlin"))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Substituted to the :core project of the included spec-test-driven build.
    implementation("org.jetbrains.kotlin.course:core:0.0.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
