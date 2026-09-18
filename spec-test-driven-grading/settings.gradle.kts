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
include(":reference-probe")
include(":reference-probe-franchise")

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

// The graded fork readings for the 11.6 capstone (./gradlew generateForks). One module per READING of
// something the brief leaves open — see forks/catalog.json for why this replaced a hidden corner-case
// set. The practice tier is the sibling block below; both are teacher-only.
file("forks").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":forks:${it.name}") }

// The PRACTICE fork tier — one fork (ties), two readings. It used to live in the handed-out capstone
// build so a learner could run it themselves; it was moved here 2026-09-18 because a reading is a
// COMPLETE implementation of the feature, so shipping its source put our answer to all four graded forks
// in the learner's folder — and any agent pointed at that folder would read it. Same failure that moved
// the briefs out. The teacher demonstrates it in class instead.
file("forks-practice").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":forks-practice:${it.name}") }

// Implementations written by an agent from a specification alone (11.4, exercise step 3e), one
// module per (agent, specification) pair: implementations/<agent>/<written-claude-code>/.
file("implementations").listFiles()
    ?.filter { it.isDirectory }
    ?.sortedBy { it.name }
    ?.forEach { agent ->
        agent.listFiles()
            ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
            ?.sortedBy { it.name }
            ?.forEach { include(":implementations:${agent.name}:${it.name}") }
    }
