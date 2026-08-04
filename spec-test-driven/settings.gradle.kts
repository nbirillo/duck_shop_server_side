pluginManagement {
    // The convention plugins (duck-shop.solution) live in the build-logic included build.
    includeBuild("build-logic")
}

rootProject.name = "spec-test-driven"

include(":core", ":starter")

// The reference implementation and grading suite live in a SEPARATE build OUTSIDE this folder
// (../spec-test-driven-grading), so a student's project never contains the answers — not even as
// readable files. This build knows nothing about grading.

// Auto-discover folders under solutions/ and exercises/: every subdir with a build script
// becomes a module. Adding one is just dropping a folder — no edit here.
fun autoDiscover(dir: String) = file(dir).listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":$dir:${it.name}") }

autoDiscover("solutions")   // agent implementations of the impl exercise
autoDiscover("exercises")   // learner-facing exercises (e.g. write-tests)
autoDiscover("test-suites") // agent-generated test suites (runAgent -Pmode=tests)
autoDiscover("hardened")    // agent-hardened suites (runAgent -Pmode=verify-harden)
autoDiscover("mutants")     // mutation testing: :core with one injected defect (11.2, generateMutants)
autoDiscover("variants")    // conformance check: :core rewritten without changing what it decides (11.2 advanced)
