// :grading — the teacher-only reference implementation + the shared acceptance suite. Depends on
// :core from the sibling spec-test-driven build (substituted via the composite includeBuild) and
// reuses the single shared test source by relative path. Not the duck-shop.solution convention
// plugin: this build wires :core as a substituted dependency, not as a project of the student build.
// The Kotlin plugin version is declared once in the root build script.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

    sourceSets.named("test") {
        // The exact same shared acceptance suite the student implementation modules run.
        kotlin.srcDir(rootDir.resolve("../spec-test-driven/tests/kotlin"))
        // Plus the acceptance suite for the GIVEN policy algebra. It lives here, not in the student
        // folder: its test names spell out the boundary and vacuous-truth cases that exercise 11.2
        // asks the learner to discover, so in the student folder it was an answer key.
        kotlin.srcDir(rootDir.resolve("tests/kotlin"))
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
