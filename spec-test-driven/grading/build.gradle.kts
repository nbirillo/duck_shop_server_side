// :grading — TEACHER-only reference implementation plus the reference acceptance tests.
// Included in the build only with -PincludeGrading, so a student's default project never
// contains the reference answers.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
