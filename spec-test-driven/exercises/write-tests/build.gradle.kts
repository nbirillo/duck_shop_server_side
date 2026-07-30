// 11.2 — the "write tests" exercise. The admission-policy algebra is GIVEN and correct in :core;
// the learner writes tests here (under src/test) that pin its behaviour and would catch bugs.
// This module runs the LEARNER'S OWN tests (not the authored acceptance suite), against :core.

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
