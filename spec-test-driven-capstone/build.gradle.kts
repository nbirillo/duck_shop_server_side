// Root of the capstone build. It carries no answer key: the graded fork readings, the reference and
// the property catalogs all live in spec-test-driven-grading/, which is not handed out.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
    id("duck-shop.mutants")
}
