// Convention for any "implementation" module — :starter, :grading and every solutions/<agent>/.
// Each such module supplies only its own policy implementation under src/main; this plugin
// wires in the shared contract (:core) and the SINGLE shared acceptance suite (tests/kotlin),
// so the exact same tests are compiled and run against every implementation (Design B).

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    // Attach the one shared acceptance suite to this module's test source set. Because each
    // module compiles it separately against its own concrete types, there are no collisions.
    sourceSets.named("test") {
        kotlin.srcDir(rootDir.resolve("tests/kotlin"))
    }
}

dependencies {
    "implementation"(project(":core"))
    "testImplementation"(kotlin("test"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
