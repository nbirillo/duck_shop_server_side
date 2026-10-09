pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    // The convention plugins are shared with the other two builds, so the tasks behave identically.
    includeBuild("../spec-test-driven/build-logic")
}

// THE CAPSTONE, and a build of its own rather than a folder inside spec-test-driven/ — for one
// concrete reason. What the capstone hands over includes a WORKING `priceFor`, because pricing was
// settled back in the spec-writing exercise and any real codebase would already have it. Committing
// that file inside the student folder would give away the answer to the implement-from-specification
// step to anyone still working through the spec-writing exercise — and to any agent working in that
// folder. Same reasoning that put the grading build and the business briefs outside it.
//
// So this build is HANDED OUT when the capstone starts, and it borrows :core from the sibling build the
// same way the grading build does. The link points only from here INTO spec-test-driven, never back.
includeBuild("../spec-test-driven")

rootProject.name = "spec-test-driven-capstone"

// What you inherit: a specification, an implementation written from it, and the tests that came with it.
include(":inherited")

// Where your own work goes. Empty until you put something in it.
include(":work")
