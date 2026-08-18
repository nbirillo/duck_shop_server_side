// Registers the mutation-testing tasks on the project it is applied to (the root of a build).
// Applied by both the student build and the teacher-only grading build; the paths each build uses
// come from gradle properties (mutantsCatalog / mutantsCoreSrc / mutantsLearnerTests / mutantsOut),
// so the task code is shared.
//
// Catalogs, directions, one engine:
//   mutants/  — injected defects, a faithful suite KILLS them (exercise 11.2, both tiers)
//   variants/ — legal refactorings, a faithful suite stays GREEN on them (11.2 advanced tier only)
//   forks/    — READINGS of what the brief leaves open; a suite SETTLES a fork by accepting exactly
//               one of them (the 11.6 capstone). The mirror of variants/: that one says "do not pin
//               what is free", this one says "do pin what is a decision".

import duckshop.AttackReportTask
import duckshop.ForkReportTask
import duckshop.GenerateForksTask
import duckshop.GenerateMutantsTask
import duckshop.MutationReportTask
import duckshop.PrepareAttackTask

tasks.register<GenerateMutantsTask>("generateMutants") {
    group = "duck-shop"
    description = "Regenerate the mutant modules from the mutant catalog (mutation testing)."
    catalogPath.convention(providers.gradleProperty("mutantsCatalog").orElse("mutants/catalog.json"))
    outPath.convention(providers.gradleProperty("mutantsOut").orElse("mutants"))
    alsoCompile.convention(providers.gradleProperty("mutantsAlsoCompile").orElse(""))
    packagePath.convention(providers.gradleProperty("mutantsPackage")
        .orElse("org/jetbrains/kotlin/course/duck/shop/admission"))
    defaultTests.convention("learner")
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
    alsoCompile.convention(providers.gradleProperty("mutantsAlsoCompile").orElse(""))
    packagePath.convention(providers.gradleProperty("mutantsPackage")
        .orElse("org/jetbrains/kotlin/course/duck/shop/admission"))
    defaultTests.convention("learner")
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

tasks.register<GenerateForksTask>("generateForks") {
    group = "duck-shop"
    description = "Regenerate one module per READING of a fork the capstone brief leaves open."
    catalogPath.convention(providers.gradleProperty("forksCatalog").orElse("forks/catalog.json"))
    outPath.convention(providers.gradleProperty("forksOut").orElse("forks"))
    coreSrc.convention(providers.gradleProperty("forksCoreSrc").orElse("core/src/main/kotlin"))
    // No default that could be a correct answer: the student build has no priceFor to point at until
    // the capstone hands one over, and quietly falling back to the reference would leak 11.4.
    pricingSrc.convention(providers.gradleProperty("forksPricing").orElse(""))
    testsSrc.convention(providers.gradleProperty("forkTests").orElse(""))
    packagePath.convention(providers.gradleProperty("forksPackage")
        .orElse("org/jetbrains/kotlin/course/duck/shop/pricing"))
}

tasks.register<ForkReportTask>("verifyForks") {
    group = "verification"
    description = "Capstone: report which forks the suite SETTLES and which it leaves open. " +
        "Params: [-PforkTests=<test source dir>] [-PforksStrict]."
    catalogPath.convention(providers.gradleProperty("forksCatalog").orElse("forks/catalog.json"))
    outPath.convention(providers.gradleProperty("forksOut").orElse("forks"))
    dependsOn(subprojects.filter { it.path.startsWith(":forks:") }.map { "${it.path}:test" })
}

// For an interactive agent, which writes the attacking sources itself and has no API call to hang
// the scaffolding off. runAgent -Pmode=attack does the same thing for an API agent.
tasks.register<PrepareAttackTask>("prepareAttack") {
    group = "duck-shop"
    description = "Turn hand-placed sources in attacks/<agent>/src/main into a module verifyAttack can " +
        "score. Params: -Pagent=<name> [-PmutantTests=<dir>]."
    agent.convention(providers.gradleProperty("agent"))
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
