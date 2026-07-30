// :core — the stable contract: the AdmissionPolicy interface and the Duck/Accessory/Shop
// domain. Both :starter and :grading depend on it; it holds no policy implementations.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}
