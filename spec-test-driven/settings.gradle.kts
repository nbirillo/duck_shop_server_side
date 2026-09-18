pluginManagement {
    // The convention plugins (duck-shop.mutants, duck-shop.run-agent, duck-shop.spec) live in the
    // build-logic included build.
    includeBuild("build-logic")
}

rootProject.name = "spec-test-driven"

include(":core")

// The reference implementation and the graded catalogs live in a SEPARATE build OUTSIDE this folder
// (../spec-test-driven-grading). What that buys is precise, and it is worth stating precisely: the
// learner's PROJECT does not contain the answers, so the agent they point at this folder cannot read
// them. It is not a lock — a clone of the repository has the sibling directory too, and keeping it
// unread is on the learner. This build knows nothing about grading either way.

// Auto-discover folders under exercises/ and the generated dirs: every subdir with a build script
// becomes a module. Adding one is just dropping a folder — no edit here.
fun autoDiscover(dir: String) = file(dir).listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":$dir:${it.name}") }

autoDiscover("exercises")   // learner-facing exercises (e.g. write-tests)
autoDiscover("test-suites") // agent-generated test suites (runAgent -Pmode=tests)
autoDiscover("hardened")    // agent-hardened suites (runAgent -Pmode=verify-harden)
autoDiscover("mutants")     // mutation testing: :core with one injected defect (11.2, generateMutants)
autoDiscover("variants")    // conformance check: :core rewritten without changing what it decides (11.2 advanced)
autoDiscover("attacks")     // adversary: implementations that try to pass the suite and still be wrong (11.2 advanced)

// Implementations an agent wrote in a sandbox and `prepareImplementation` collected, one module per
// (agent, specification). These exist so two readings of the same spec can be compared; they hold no
// answer key, so they belong on the learner's side.
file("implementations").listFiles()
    ?.filter { it.isDirectory }
    ?.sortedBy { it.name }
    ?.forEach { agent ->
        agent.listFiles()
            ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
            ?.sortedBy { it.name }
            ?.forEach { include(":implementations:${agent.name}:${it.name}") }
    }

