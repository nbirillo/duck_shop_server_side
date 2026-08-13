pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // The mutation-testing convention plugin (duck-shop.mutants) is shared with the student build.
    includeBuild("../spec-test-driven/build-logic")
}

// TEACHER-ONLY grading build. It lives OUTSIDE the student's spec-test-driven/ folder, so a
// student's agent/IDE working from that folder never sees the reference implementation — nor the
// graded mutant catalog, which would otherwise enumerate exactly which cases to test. It pulls
// :core (and the shared test source) from the sibling build via a composite build — the link
// only points from here INTO spec-test-driven, never the other way, so the student stays clean.
includeBuild("../spec-test-driven")

rootProject.name = "spec-test-driven-grading"

include(":grading")
include(":reference")

// Auto-discover the generated mutant modules (./gradlew generateMutants), mirroring the student build.
file("mutants").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":mutants:${it.name}") }

// The same, for the 11.4 pricing mutants — they mutate :reference rather than :core.
file("pricing-mutants").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":pricing-mutants:${it.name}") }
