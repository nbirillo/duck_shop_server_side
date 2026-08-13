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
        "Params: -Pspec=<SPEC.md or a directory of them>."
    surfaceFile.convention(
        providers.gradleProperty("specSurface")
            .orElse("exercises/write-spec/README.md"),
    )
}

tasks.register<ExtractionScoreTask>("scoreExtraction") {
    group = "verification"
    description = "Score the claim extractor against the hand-authored key — how often layer 2 is " +
        "right, as opposed to merely repeatable. Params: -Pagent=<name> [-Pkey=<key.json>]."
    keyPath.convention(providers.gradleProperty("key").orElse("fixtures/11.4/key.json"))
}

tasks.register<duckshop.SandboxTask>("sandbox") {
    group = "duck-shop"
    description = "Lay out a folder with the data types and your SPEC.md and nothing else, to point " +
        "an agent at. Params: -Pspec=<SPEC.md> [-Pname=<folder>]."
}
