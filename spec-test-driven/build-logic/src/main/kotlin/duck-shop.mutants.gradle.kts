// Registers the mutation-testing tasks on the project it is applied to (the root of a build).
// Applied by both the student build and the teacher-only grading build; the paths each build uses
// come from gradle properties (mutantsCatalog / mutantsCoreSrc / mutantsLearnerTests / mutantsOut),
// so the task code is shared.
//
// Two catalogs, two directions, one engine:
//   mutants/  — injected defects, a faithful suite KILLS them (exercise 11.2, both tiers)
//   variants/ — legal refactorings, a faithful suite stays GREEN on them (11.2 advanced tier only)

import duckshop.AttackReportTask
import duckshop.GenerateMutantsTask
import duckshop.MutationReportTask

tasks.register<GenerateMutantsTask>("generateMutants") {
    group = "duck-shop"
    description = "Regenerate the mutant modules from the mutant catalog (mutation testing)."
    catalogPath.convention(providers.gradleProperty("mutantsCatalog").orElse("mutants/catalog.json"))
    outPath.convention(providers.gradleProperty("mutantsOut").orElse("mutants"))
}

tasks.register<MutationReportTask>("verifyMutants") {
    group = "verification"
    description = "Mutation testing: run a suite against every mutant and report the mutation score. " +
        "Params: [-PmutantTests=learner|<test source dir>] [-PtestBudget=<n>] [-PmutantsStrict]."
    catalogPath.convention(providers.gradleProperty("mutantsCatalog").orElse("mutants/catalog.json"))
    outPath.convention(providers.gradleProperty("mutantsOut").orElse("mutants"))
    // Task paths are resolved lazily, so the mutant modules do not need to be evaluated yet.
    dependsOn(subprojects.filter { it.path.startsWith(":mutants:") }.map { "${it.path}:test" })
}

tasks.register<GenerateMutantsTask>("generateVariants") {
    group = "duck-shop"
    description = "Regenerate the conformant-variant modules from the variant catalog."
    catalogPath.convention(providers.gradleProperty("variantsCatalog").orElse("variants/catalog.json"))
    outPath.convention(providers.gradleProperty("variantsOut").orElse("variants"))
}

tasks.register<MutationReportTask>("verifyVariants") {
    group = "verification"
    description = "Conformance check: run a suite against legal refactorings of the algebra and report " +
        "every test that fails on behaviour-preserving code. Params: [-PmutantTests=learner|<dir>] " +
        "[-PmutantsStrict]."
    catalogPath.convention(providers.gradleProperty("variantsCatalog").orElse("variants/catalog.json"))
    outPath.convention(providers.gradleProperty("variantsOut").orElse("variants"))
    dependsOn(subprojects.filter { it.path.startsWith(":variants:") }.map { "${it.path}:test" })
}

// The third direction, and the only one without a ceiling: an agent writes an implementation that
// passes the suite and still contradicts the spec. Run with --continue, so an attack that does not
// compile still reaches the report.
tasks.register<AttackReportTask>("verifyAttack") {
    group = "verification"
    description = "Score one attacking implementation from attacks/<agent>/: does the suite catch it, " +
        "and does it really differ from :core? Params: -Pagent=<name> [-PmutantsStrict]."
    agent.convention(providers.gradleProperty("agent"))
    val attacked = providers.gradleProperty("agent").orNull
    listOfNotNull(attacked?.let { ":attacks:$it" }, ":attacks:reference")
        .filter { findProject(it) != null }
        .forEach { dependsOn("$it:test") }
}
