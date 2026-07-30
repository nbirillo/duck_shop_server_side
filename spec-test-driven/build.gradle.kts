// Root of the "spec-test-driven" build — a fourth, independent Gradle build in this
// repository, alongside server/, ktor-server/ and frontend/.

plugins {
    kotlin("jvm") version "2.2.20" apply false
}

// Which implementation the student's `checkPrimary` runs against. Override with
// -PprimaryAgent=<name>; a solutions/<name>/ folder, or "starter"/"grading".
val primaryAgent: String = providers.gradleProperty("primaryAgent").getOrElse("starter")

val primaryPath: String = when (primaryAgent) {
    "starter", "grading" -> ":$primaryAgent"
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
