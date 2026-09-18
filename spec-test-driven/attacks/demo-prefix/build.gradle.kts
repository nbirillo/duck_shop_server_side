// Hand-written twin of what `runAgent -Pmode=attack` generates, kept so the machinery can be shown
// (and regression-checked) without calling a model. See src/main for what it changes.

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)

    sourceSets.named("main") {
        kotlin.srcDir(rootDir.resolve("core/src/main/kotlin"))
        kotlin.exclude("**/Leaves.kt")
    }
    sourceSets.named("test") {
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
    ignoreFailures = true
}
