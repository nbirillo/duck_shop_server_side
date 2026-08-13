// The deterministic half of checking a specification (11.4). Applied by both builds: it reads text
// and declarations only, so there is nothing in it a learner should not run on their own work.

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
