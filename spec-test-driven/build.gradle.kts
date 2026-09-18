// Root of the "spec-test-driven" build — a fourth, independent Gradle build in this
// repository, alongside server/, ktor-server/ and frontend/.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
    id("duck-shop.mutants")
    id("duck-shop.spec")
}

// Exercise 11.2 feedback, kept under its original name: the hand-written practice-bugs/ copies were
// replaced by generated mutants (one engine instead of two), and `verifyMutants` is the report.
tasks.register("practiceCatch") {
    group = "duck-shop"
    description = "Alias for verifyMutants — does the learner's suite catch each seeded defect?"
    dependsOn("verifyMutants")
}
