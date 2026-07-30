pluginManagement {
    // The convention plugins (duck-shop.solution) live in the build-logic included build.
    includeBuild("build-logic")
}

rootProject.name = "spec-test-driven"

include(":core", ":starter")

// :grading holds the reference implementation and grading suite — teacher-only. Added only
// when -PincludeGrading is passed (or includeGrading=true in ~/.gradle/gradle.properties), so
// a student's default import contains the stubs (in :starter) but never the reference answers.
if (providers.gradleProperty("includeGrading").isPresent) {
    include(":grading")
}

// Auto-discover agent solutions: every solutions/<name>/ that has a build script becomes a
// module. Adding a new agent is just dropping a folder — no edit here.
file("solutions").listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.sortedBy { it.name }
    ?.forEach { include(":solutions:${it.name}") }
