// :core holds its OWN minimal domain (Duck, Accessory) and the AdmissionPolicy
// Specification-pattern types. Pure Kotlin/JVM logic — no Spring, no upstream coupling —
// so the in-memory MVP (sub-modules 11.0–11.6) stands entirely on its own.

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
    // kotlin("test") maps @Test / assert* onto the JUnit 5 platform below.
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
