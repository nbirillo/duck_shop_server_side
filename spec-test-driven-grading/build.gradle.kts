// Root build for teacher-only grading. It contains the reference implementation (:grading)
// and the graded mutants used in mutation testing. Both builds use the same mutation tasks;
// this build sets its own paths in gradle.properties.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
    id("duck-shop.mutants")
    id("duck-shop.spec")
}

// The spec-writing exercise has a separate set of pricing mutants. They change :reference rather than
// :core, so they need different source, package, and output paths. The task code stays shared.
tasks.named<duckshop.SpecReportTask>("verifySpec") {
    // The exercise files are in a sibling directory. Keep -PspecSurface as the first choice:
    // replacing the plugin's convention without it would ignore the flag and could check an
    // advanced spec against the basic requirements instead.
    surfaceFile.convention(
        providers.gradleProperty("specSurface")
            .orElse("../spec-test-driven/exercises/write-spec/README.md"),
    )
}

tasks.register<duckshop.GenerateMutantsTask>("generatePricingMutants") {
    group = "duck-shop"
    description = "Recreate the pricing mutant modules for the spec-writing exercise from pricing-mutants/catalog.json."
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    packagePath.set("org/jetbrains/kotlin/course/duck/shop/pricing")
    // Change the teacher's reference implementation, not :core.
    coreSrc.set("reference/src/main/kotlin")
    // Include :core unchanged; the reference needs Duck, DiscountRule, and AdmissionPolicy.
    alsoCompile.set("../spec-test-driven/core/src/main/kotlin")
    // Test these mutants with the pricing properties, not the admission tests from the test-writing exercise.
    defaultTests.set("pricing-properties/kotlin")
}

tasks.register<duckshop.MutationReportTask>("verifyPricingMutants") {
    group = "verification"
    description = "Run the property tests against each pricing mutant from the spec-writing exercise. " +
            "Params: [-PmutantTests=<test source dir>] [-PmutantsStrict]."
    catalogPath.set("pricing-mutants/catalog.json")
    defaultTests.set("pricing-properties/kotlin")
    outPath.set("pricing-mutants")
    // Set the order only. A report that depends on a failed compilation would be skipped.
    // See ../spec-test-driven/build-logic/reachable-reports.settings.gradle.kts.
    mustRunAfter(subprojects.filter { it.path.startsWith(":pricing-mutants:") }.map { "${it.path}:test" })
}

tasks.register<duckshop.PropertyMatrixTask>("pricingPropertyMatrix") {
    group = "verification"
    description = "Show which pricing mutants each property catches, and which properties catch none."
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    dependsOn("verifyPricingMutants")
}

// Let learners check the claims they chose themselves, rather than relying on what the AI
// extracted from their spec. After they declare the claims, the checks are deterministic.
tasks.register<duckshop.ClaimReportTask>("verifyClaims") {
    group = "verification"
    description = "Test your declared claims as properties and compare them with the AI extraction. " +
            "Params: -Pclaims=<ids or names> | claims.txt beside -Pspec, [-Pagent=<run>]."
    propertyFile.set(
        "pricing-properties/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/PricingProperties.kt",
    )
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    dependsOn("verifyPricingMutants")
}

// Only this grading build has verifyImplementations. Set the command here so the student
// build's prepareImplementation task never suggests a task that does not exist there.
tasks.named<duckshop.PrepareImplementationTask>("prepareImplementation") {
    reportCommand.set("./gradlew verifyImplementations -Pagent=<agent>")
}

// The spec-writing exercise, step 3e: see what an agent builds using only the specification.
tasks.register<duckshop.ImplementationReportTask>("verifyImplementations") {
    group = "verification"
    description = "Compare implementations built from a spec alone with the reference, " +
            "excluding out-of-scope checks. Params: -Pagent=<name> [-Pkey=<key.json>]. " +
            "Code that does not compile appears in the report instead of counting as a build failure."
    keyPath.convention(providers.gradleProperty("key").orElse("fixtures/write-spec/key.json"))
    // Look only at the requested agent. A different agent's broken module must not stop
    // this report. A broken module belonging to this agent is useful information, so
    // running with --continue should still let the report show DID NOT BUILD.
    val reported = providers.gradleProperty("agent").orNull
    // Order tasks without making the report depend on successful compilation.
    // Otherwise a failed compile would prevent the DID NOT BUILD result from appearing.
    // The settings script below schedules the tasks:
    // ../spec-test-driven/build-logic/reachable-reports.settings.gradle.kts.
    mustRunAfter(
        subprojects
            .filter { it.path.startsWith(":implementations:${reported ?: ""}") && it.buildFile.exists() }
            .map { "${it.path}:test" },
    )
}
