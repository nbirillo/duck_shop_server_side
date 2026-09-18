// The deterministic half of checking a specification (11.4). Applied by both builds: it reads text
// and declarations only, so there is nothing in it a learner should not run on their own work.
//
// scoreExtraction is the exception and is teacher-only in practice: it needs the hand-authored key,
// which lives with the fixtures in the grading build.

import duckshop.ExtractionScoreTask
import duckshop.SpecReportTask

tasks.register<SpecReportTask>("verifySpec") {
    group = "verification"
    description = "Check a specification's structure, length and surface coverage. " +
        "Params: -Pspec=<SPEC.md or a directory of them> [-Pparts, if that directory is ONE " +
        "specification split into parts rather than several separate ones]."
    surfaceFile.convention(
        providers.gradleProperty("specSurface")
            .orElse("exercises/write-spec/README.md"),
    )
}

// Teacher-only: it scores the claim extractor against the hand-authored key, and the key lives with
// the fixtures in the grading build. Registering it where there is no key put a task in a learner's
// `gradlew tasks` that can only fail — and advertised the key while it is meant to be out of sight.
if (file("fixtures/11.4/key.json").isFile || providers.gradleProperty("key").isPresent) {
tasks.register<ExtractionScoreTask>("scoreExtraction") {
    group = "verification"
    description = "Score the claim extractor against the hand-authored key — how often layer 2 is " +
        "right, as opposed to merely repeatable. Params: -Pagent=<name> [-Pkey=<key.json>]."
    keyPath.convention(providers.gradleProperty("key").orElse("fixtures/11.4/key.json"))
}
}

tasks.register<duckshop.SandboxTask>("sandbox") {
    group = "duck-shop"
    description = "Lay out a folder with the data types and your SPEC.md and nothing else, to point " +
        "an agent at. Params: -Pspec=<SPEC.md> [-Pname=<folder>]."
}

// Collect what an agent wrote in a sandbox into a module that can be recorded and compared.
// Learner-side: the paths come from each build's gradle.properties, and in the student build the
// property catalog is deliberately absent.
tasks.register<duckshop.PrepareImplementationTask>("prepareImplementation") {
    group = "duck-shop"
    description = "Take the implementation an agent wrote in a sandbox so it can be compared. " +
        "Params: -Pagent=<name> -Pspec=<the spec> [-Pfrom=sandbox/<name>]."
    // verifyDivergence is registered just below, so it is available wherever this task is. The
    // grading build overrides this with verifyImplementations, which exists only there.
    reportCommand.convention(
        "./gradlew verifyDivergence -PagentA=<agent> -PagentB=<a second run of the same specification>",
    )
}

// This plugin registers verifyDivergence, so this plugin is what tells runAgent to point at it.
tasks.withType<duckshop.RunAgentTask>().configureEach {
    implNextStep.set(
        "./gradlew verifyDivergence -PagentA=<agent> -PagentB=<a second run of the " +
            "same specification>   (where two readings disagree is what your text left open)",
    )
}

// How much did your specification leave to chance? Two readers, compared. Needs no reference and no
// answer key, which is exactly why a learner can run it on their own work.
tasks.register<duckshop.DivergenceReportTask>("verifyDivergence") {
    group = "verification"
    description = "Compare two independent implementations of the same specification. " +
        "Params: -PagentA=<name> -PagentB=<name> [-Pspec=<one>] [-Pexamples=n]."
    referenceRecording.convention(
        providers.gradleProperty("referenceRecording").orElse("reference-probe/recording.txt"),
    )
    // Record before comparing. Without this the documented sequence ends in "No recordings for
    // reader-a / reader-b. Run their :test tasks first." — a step the learner has no reason to know
    // about, and one more command on a slide that already has four.
    listOfNotNull(providers.gradleProperty("agentA").orNull, providers.gradleProperty("agentB").orNull)
        .flatMap { agent ->
            subprojects.filter { it.path.startsWith(":implementations:$agent:") && it.buildFile.exists() }
        }
        .forEach { dependsOn("${it.path}:test") }
}
