rootProject.name = "spec-test-driven"

include(":core", ":starter")

// :grading holds the reference implementation and the grading suite — teacher-only. It is
// added to the build only when -PincludeGrading is passed, so a student's default import
// contains the stubs (in :starter) but never the reference answers.
if (providers.gradleProperty("includeGrading").isPresent) {
    include(":grading")
}
