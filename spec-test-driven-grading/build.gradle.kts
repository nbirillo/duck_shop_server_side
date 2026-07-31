// Root of the teacher-only grading build. Holds the reference implementation (:grading) and the
// GRADED mutant set for mutation testing. The mutation tasks come from the same convention plugin
// the student build uses; the paths this build feeds them are in gradle.properties.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.mutants")
}
