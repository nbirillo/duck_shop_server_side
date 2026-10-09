// :reference — the teacher-only reference implementation of the pricing feature.
//
// It lives here rather than in :core for the same reason :grading does: a learner who can read it
// does not have to specify anything, and the spec-writing exercise is the specifying. :core carries only the
// DiscountRule data types, which the brief shows the learner anyway.

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
    implementation("org.jetbrains.kotlin.course:core:0.0.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
