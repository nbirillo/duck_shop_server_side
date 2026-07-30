// :starter — the student-facing surface: stub policy types (TODO()) plus the test-first
// tests. A student (or an AI agent) implements the stubs until `:starter:test` goes green.

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
