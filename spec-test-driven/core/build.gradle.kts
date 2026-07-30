// :core — the stable contract: the AdmissionPolicy interface and the Duck/Accessory/Shop
// domain. Both :starter and :grading depend on it; it holds no policy implementations.

plugins {
    kotlin("jvm")
}

// Coordinates so the sibling grading build can depend on :core via composite-build substitution.
group = "org.jetbrains.kotlin.course"
version = "0.0.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}
