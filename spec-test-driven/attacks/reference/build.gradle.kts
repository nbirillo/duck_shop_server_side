// The reference side of the differential check: the real :core algebra, unmodified, running the
// same probe every attacks/<agent>/ module runs. verifyAttack diffs the two probe.txt files.
//
// Hand-written and committed, unlike the attack modules, which runAgent -Pmode=attack generates.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

    sourceSets.named("main") {
        kotlin.srcDir(rootDir.resolve("core/src/main/kotlin"))
    }
    sourceSets.named("test") {
        // The suite runs here too, against the correct algebra: a test that is red on :core would
        // otherwise look like the suite catching the attack. verifyAttack subtracts these.
        kotlin.srcDir(
            rootDir.resolve(
                when (val selected = providers.gradleProperty("mutantTests").getOrElse("learner")) {
                    "learner" -> "exercises/write-tests/src/test/kotlin"
                    else -> selected
                },
            ),
        )
        kotlin.srcDir(rootDir.resolve("tools/probe/kotlin"))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    // A suite that is red on :core is a fact to report, not a reason to stop before the probe runs.
    ignoreFailures = true
}
