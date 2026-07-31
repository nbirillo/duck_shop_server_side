// Registers the mutation-testing tasks on the project it is applied to (the root of a build).
// Applied by both the student build and the teacher-only grading build; the paths each build uses
// come from gradle properties (mutantsCatalog / mutantsCoreSrc / mutantsLearnerTests / mutantsOut),
// so the task code is shared.

import duckshop.GenerateMutantsTask
import duckshop.MutationReportTask

tasks.register<GenerateMutantsTask>("generateMutants") {
    group = "duck-shop"
    description = "Regenerate the mutant modules from the mutant catalog (mutation testing)."
}

tasks.register<MutationReportTask>("verifyMutants") {
    group = "verification"
    description = "Mutation testing: run a suite against every mutant and report the mutation score. " +
        "Params: [-PmutantTests=learner|<test source dir>] [-PmutantsStrict]."
    // Task paths are resolved lazily, so the mutant modules do not need to be evaluated yet.
    dependsOn(subprojects.filter { it.path.startsWith(":mutants:") }.map { "${it.path}:test" })
}
