// Root of the "spec-test-driven" build — a fourth, independent Gradle build in this
// repository, alongside server/, ktor-server/ and frontend/.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
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

// Exercise 11.2 feedback: run the learner's tests (from exercises/write-tests) against each
// broken algebra under practice-bugs/. A good suite CATCHES every bug (some test fails there).
tasks.register("practiceCatch") {
    group = "duck-shop"
    description = "Check the learner's write-tests suite catches each practice-bugs/ broken algebra."
    val bugProjects = subprojects.filter { it.path.startsWith(":practice-bugs:") }
    dependsOn(bugProjects.map { "${it.path}:test" })
    doLast {
        logger.lifecycle("practiceCatch — does your suite catch each seeded bug?")
        var missed = 0
        bugProjects.sortedBy { it.name }.forEach { p ->
            val dir = p.layout.buildDirectory.dir("test-results/test").get().asFile
            val xmls = dir.listFiles { f -> f.name.startsWith("TEST-") && f.extension == "xml" } ?: emptyArray()
            val failures = xmls.sumOf { xml ->
                Regex("failures=\"(\\d+)\"").find(xml.readText())?.groupValues?.get(1)?.toInt() ?: 0
            }
            val ran = xmls.isNotEmpty()
            when {
                !ran -> logger.lifecycle("  ${p.name}: ERROR — your tests did not compile/run against it")
                failures > 0 -> logger.lifecycle("  ${p.name}: CAUGHT")
                else -> { logger.lifecycle("  ${p.name}: MISSED — add a test for this case"); missed++ }
            }
        }
        if (missed > 0) logger.lifecycle("$missed bug(s) not caught — strengthen your tests.")
        else logger.lifecycle("All bugs caught. ✅")
    }
}

// Convenience for the teacher: after driving an interactive agent that filled in the :starter
// stubs, restore them to the committed TODO() state. Touches only starter/src. Requires git.
tasks.register<Exec>("resetStarter") {
    group = "duck-shop"
    description = "Restore :starter to its committed TODO() stubs (e.g. after an interactive agent run)."
    workingDir = rootDir
    commandLine("git", "restore", "--source=HEAD", "--staged", "--worktree", "--", "starter/src")
}
