pluginManagement {
    // The convention plugins (duck-shop.solution) live in the build-logic included build.
    includeBuild("build-logic")
}

rootProject.name = "spec-test-driven"

include(":core", ":starter")

// The reference implementation and grading suite live in a SEPARATE build OUTSIDE this folder
// (../spec-test-driven-grading), so a student's project never contains the answers — not even as
// readable files. This build knows nothing about grading.

// Auto-discover agent solutions: every solutions/<name>/ that has a build script becomes a
// module. Adding a new agent is just dropping a folder — no edit here.
file("solutions").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":solutions:${it.name}") }
