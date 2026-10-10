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

// A compile failure must leave NO results behind. `test` never starts when compilation fails, so
// the previous run's XML would still be sitting there and the report would print that score for a
// suite that no longer builds — measured: a clean "1/4 killed" for a suite with a type error in it.
// Clearing as compilation STARTS is what makes the report's "did not compile" branch mean it.
val resultsDir = layout.buildDirectory.dir("test-results/test")
listOf("compileKotlin", "compileTestKotlin").forEach { stage ->
    tasks.named(stage) { doFirst { resultsDir.get().asFile.deleteRecursively() } }
}
