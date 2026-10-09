// Adds the mutation-testing tasks to the root project.
// Both the student build and the teacher's grading build use these tasks. Each build sets its
// own paths through Gradle properties, so they can share the same task definitions.
//
// Three catalogs, three kinds of checks, one shared setup:
//   mutants/  — broken implementations. Good tests should catch them (11.2, both tiers).
//   variants/ — valid refactorings. Good tests should still pass (11.2, advanced tier).
//   forks/    — different valid ways to fill gaps in the brief. In the 11.6 capstone, tests
//               settle a fork when they accept exactly one reading. Variants check that tests
//               leave flexible behavior flexible; forks check that tests make choices explicit.

import duckshop.AttackReportTask
import duckshop.ForkReportTask
import duckshop.GenerateForksTask
import duckshop.GenerateMutantsTask
import duckshop.MutationReportTask
import duckshop.PrepareAttackTask

tasks.register<GenerateMutantsTask>("generateMutants") {
    group = "duck-shop"
    description = "Rebuild the mutant modules from the catalog for mutation testing."
    catalogPath.convention(providers.gradleProperty("mutantsCatalog").orElse("mutants/catalog.json"))
    outPath.convention(providers.gradleProperty("mutantsOut").orElse("mutants"))
    alsoCompile.convention(providers.gradleProperty("mutantsAlsoCompile").orElse(""))
    packagePath.convention(providers.gradleProperty("mutantsPackage")
        .orElse("org/jetbrains/kotlin/course/duck/shop/admission"))
    defaultTests.convention("learner")
}

tasks.register<MutationReportTask>("verifyMutants") {
    group = "verification"
    description = "Check the test suite against every mutant and show how many it catches. " +
            "Params: [-PmutantTests=learner|<test source dir>] [-PtestBudget=<n>] [-PmutantsStrict]."
    catalogPath.convention(providers.gradleProperty("mutantsCatalog").orElse("mutants/catalog.json"))
    outPath.convention(providers.gradleProperty("mutantsOut").orElse("mutants"))
    // Resolve task paths only when needed; the mutant modules may not be loaded yet.
    // Use mustRunAfter, not dependsOn. If a test fails to compile, the report should still run
    // and explain the result instead of being skipped. See reachable-reports.settings.gradle.kts.
    mustRunAfter(subprojects.filter { it.path.startsWith(":mutants:") }.map { "${it.path}:test" })
}

tasks.register<GenerateMutantsTask>("generateVariants") {
    group = "duck-shop"
    description = "Rebuild the modules containing valid refactorings from the variant catalog."
    catalogPath.convention(providers.gradleProperty("variantsCatalog").orElse("variants/catalog.json"))
    outPath.convention(providers.gradleProperty("variantsOut").orElse("variants"))
    alsoCompile.convention(providers.gradleProperty("mutantsAlsoCompile").orElse(""))
    packagePath.convention(providers.gradleProperty("mutantsPackage")
        .orElse("org/jetbrains/kotlin/course/duck/shop/admission"))
    defaultTests.convention("learner")
}

tasks.register<MutationReportTask>("verifyVariants") {
    group = "verification"
    description = "Run the suite against valid refactorings and show tests that fail even though " +
            "the behavior is unchanged. Params: [-PmutantTests=learner|<dir>] " +
            "[-PmutantsStrict]."
    catalogPath.convention(providers.gradleProperty("variantsCatalog").orElse("variants/catalog.json"))
    outPath.convention(providers.gradleProperty("variantsOut").orElse("variants"))
    // Set the order without making the tests dependencies. See verifyMutants.
    mustRunAfter(subprojects.filter { it.path.startsWith(":variants:") }.map { "${it.path}:test" })
}

tasks.register<GenerateForksTask>("generateForks") {
    group = "duck-shop"
    description = "Build one module for each possible reading of an open choice in the capstone brief."
    catalogPath.convention(providers.gradleProperty("forksCatalog").orElse("forks/catalog.json"))
    outPath.convention(providers.gradleProperty("forksOut").orElse("forks"))
    coreSrc.convention(providers.gradleProperty("forksCoreSrc").orElse("core/src/main/kotlin"))
    // Do not provide a default pricing implementation. Students do not have priceFor until
    // the capstone gives them one, and falling back to the reference would reveal the 11.4 answer.
    pricingSrc.convention(providers.gradleProperty("forksPricing").orElse(""))
    testsSrc.convention(providers.gradleProperty("forkTests").orElse(""))
    packagePath.convention(providers.gradleProperty("forksPackage")
        .orElse("org/jetbrains/kotlin/course/duck/shop/pricing"))
}

tasks.register<ForkReportTask>("verifyForks") {
    group = "verification"
    description = "Show which capstone forks the tests settle and which choices are still open. " +
            "Params: [-PforkTests=<test source dir>] [-PforksStrict]."
    catalogPath.convention(providers.gradleProperty("forksCatalog").orElse("forks/catalog.json"))
    outPath.convention(providers.gradleProperty("forksOut").orElse("forks"))
    // Use the configured output directory, not a hard-coded :forks: path. Otherwise a second
    // catalog (such as forks-practice/) could produce a report before its tests had run.
    val forksPrefix = ":" + providers.gradleProperty("forksOut").getOrElse("forks") + ":"
    // Order only, as in verifyMutants. This report must still show DID NOT COMPILE when
    // the test suite cannot compile against one of the readings.
    mustRunAfter(subprojects.filter { it.path.startsWith(forksPrefix) }.map { "${it.path}:test" })
}

// For an interactive agent that writes attack source files directly. API-based agents can
// use runAgent -Pmode=attack to prepare the same setup.
tasks.register<PrepareAttackTask>("prepareAttack") {
    group = "duck-shop"
    description = "Turn sources in attacks/<agent>/src/main into a module that verifyAttack can " +
            "check. Params: -Pagent=<name> [-PmutantTests=<dir>]."
    agent.convention(providers.gradleProperty("agent"))
}

// Another way to test the suite: an agent writes code that passes the tests but breaks the
// specification. Even if that code does not compile, the report still runs. See
// build-logic/reachable-reports.settings.gradle.kts for why --continue alone is not enough.
tasks.register<AttackReportTask>("verifyAttack") {
    group = "verification"
    description = "Check one implementation in attacks/<agent>/: do the tests catch it, and " +
            "does it differ from :core? Params: -Pagent=<name> [-PmutantsStrict]."
    agent.convention(providers.gradleProperty("agent"))
    val attacked = providers.gradleProperty("agent").orNull
    listOfNotNull(attacked?.let { ":attacks:$it" }, ":attacks:reference")
        .filter { findProject(it) != null }
        .forEach { mustRunAfter("$it:test") }
}
