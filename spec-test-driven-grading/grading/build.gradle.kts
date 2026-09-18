// :grading — the teacher-only acceptance suite for the GIVEN policy algebra, run against :core from
// the sibling spec-test-driven build (substituted via the composite includeBuild). It holds no
// sources of its own: the 11.4 reference implementation lives in :reference, not here.
// The Kotlin plugin version is declared once in the root build script.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

    sourceSets.named("test") {
        // The acceptance suite for the given policy algebra. It lives here, not in the student
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
