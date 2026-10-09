// Adds the specification checks for the spec-writing exercise. Both builds use these tasks because they
// work with specifications and claim lists, not hidden reference implementations.
//
// scoreExtraction is mainly for teachers: it needs the manually checked answer key
// from the grading build.

import duckshop.ExtractionScoreTask
import duckshop.SpecReportTask

tasks.register<SpecReportTask>("verifySpec") {
    group = "verification"
    description = "Check a specification's structure, length, and basic coverage. " +
            "Params: -Pspec=<SPEC.md or a directory of them> [-Pparts, if the directory contains " +
            "one specification split into parts instead of several separate specs]."
    surfaceFile.convention(
        providers.gradleProperty("specSurface")
            .orElse("exercises/write-spec/README.md"),
    )
}

// Only add this task when an answer key is available. Otherwise students would see
// a task they cannot run, and the hidden key would be unnecessarily exposed.
if (file("fixtures/write-spec/key.json").isFile || providers.gradleProperty("key").isPresent) {
    tasks.register<ExtractionScoreTask>("scoreExtraction") {
        group = "verification"
        description = "Compare the AI claim extractor with the manually checked answer key. " +
                "This measures accuracy, not just consistency. Params: -Pagent=<name> [-Pkey=<key.json>]."
        keyPath.convention(providers.gradleProperty("key").orElse("fixtures/write-spec/key.json"))
    }
}

tasks.register<duckshop.SandboxTask>("sandbox") {
    group = "duck-shop"
    description = "Create a folder with only the data types and your SPEC.md for an AI agent to use. " +
            "Params: -Pspec=<SPEC.md> [-Pname=<folder>]."
}

// Take the code an agent wrote in its sandbox and prepare it for comparison.
// Students can use this too. Each build supplies its own paths through gradle.properties,
// and the student build intentionally has no property catalog.
tasks.register<duckshop.PrepareImplementationTask>("prepareImplementation") {
    group = "duck-shop"
    description = "Prepare an agent's sandbox implementation so you can compare it with another. " +
            "Params: -Pagent=<name> -Pspec=<the spec> [-Pfrom=sandbox/<name>]."
    // verifyDivergence is available in both builds. The grading build instead points
    // to verifyImplementations, which is only available to teachers.
    reportCommand.convention(
        "./gradlew verifyDivergence -PagentA=<agent> -PagentB=<a second run of the same specification>",
    )
}

// Since this plugin adds verifyDivergence, it also tells runAgent to suggest that task next.
tasks.withType<duckshop.RunAgentTask>().configureEach {
    implNextStep.set(
        "./gradlew verifyDivergence -PagentA=<agent> -PagentB=<a second run of the " +
                "same specification>   (differences between the two show what your spec left open)",
    )
}

// Compare two independent implementations to see where the specification leaves room
// for different choices. Students need neither a reference implementation nor an answer key.
tasks.register<duckshop.DivergenceReportTask>("verifyDivergence") {
    group = "verification"
    description = "Compare two independent implementations built from the same specification. " +
            "Params: -PagentA=<name> -PagentB=<name> [-Pspec=<one>] [-Pexamples=n]."
    referenceRecording.convention(
        providers.gradleProperty("referenceRecording").orElse("reference-probe/recording.txt"),
    )
    // Run the tests before comparing recordings. Otherwise students would get a
    // missing-recordings error and have to discover an extra command on their own.
    listOfNotNull(providers.gradleProperty("agentA").orNull, providers.gradleProperty("agentB").orNull)
        .flatMap { agent ->
            subprojects.filter { it.path.startsWith(":implementations:$agent:") && it.buildFile.exists() }
        }
        .forEach { dependsOn("${it.path}:test") }
}
