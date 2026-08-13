// Root of the "spec-test-driven" build — a fourth, independent Gradle build in this
// repository, alongside server/, ktor-server/ and frontend/.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
    id("duck-shop.mutants")
    id("duck-shop.spec")
}

// Which implementation the student's `checkPrimary` runs against. Override with
// -PprimaryAgent=<name>: "starter" or a solutions/<name>/ folder.
val primaryAgent: String = providers.gradleProperty("primaryAgent").getOrElse("starter")

val primaryPath: String = when (primaryAgent) {
    "starter" -> ":starter"
    else -> ":solutions:$primaryAgent"
}

tasks.register("checkPrimary") {
    group = "verification"
    description = "Runs the acceptance suite against the primary implementation (primaryAgent=$primaryAgent)."
    dependsOn("$primaryPath:test")
}

tasks.register("compareAgents") {
    group = "verification"
    description = "Runs the acceptance suite against every solutions/<agent>/ implementation."
    dependsOn(
        subprojects
            .filter { it.path.startsWith(":solutions:") }
            .map { "${it.path}:test" }
    )
}

// Exercise 11.2 feedback, kept under its original name: the hand-written practice-bugs/ copies were
// replaced by generated mutants (one engine instead of two), and `verifyMutants` is the report.
tasks.register("practiceCatch") {
    group = "duck-shop"
    description = "Alias for verifyMutants — does the learner's suite catch each seeded defect?"
    dependsOn("verifyMutants")
}

// Convenience for the teacher: after driving an interactive agent that filled in the :starter
// stubs, restore them to the committed TODO() state. Touches only starter/src. Requires git.
tasks.register<Exec>("resetStarter") {
    group = "duck-shop"
    description = "Restore :starter to its committed TODO() stubs (e.g. after an interactive agent run)."
    workingDir = rootDir
    commandLine("git", "restore", "--source=HEAD", "--staged", "--worktree", "--", "starter/src")
}
