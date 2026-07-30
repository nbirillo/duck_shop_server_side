pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// TEACHER-ONLY grading build. It lives OUTSIDE the student's spec-test-driven/ folder, so a
// student's agent/IDE working from that folder never sees the reference implementation. It pulls
// :core (and the shared test source) from the sibling build via a composite build — the link
// only points from here INTO spec-test-driven, never the other way, so the student stays clean.
includeBuild("../spec-test-driven")

rootProject.name = "spec-test-driven-grading"

include(":grading")
