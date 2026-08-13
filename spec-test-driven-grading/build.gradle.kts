// Root of the teacher-only grading build. Holds the reference implementation (:grading) and the
// GRADED mutant set for mutation testing. The mutation tasks come from the same convention plugin
// the student build uses; the paths this build feeds them are in gradle.properties.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
    id("duck-shop.mutants")
    id("duck-shop.spec")
}

// The 11.4 pricing mutants are a second catalog in this build: they mutate :reference, not :core, so
// they need their own source root, package and output dir. Same task classes, different wiring —
// the shared convention plugin stays generic.
tasks.named<duckshop.SpecReportTask>("verifySpec") {
    // This build sits one level over, so the surface file is across the sibling. Keep -PspecSurface
    // ahead of it: setting a bare convention here REPLACES the plugin's, which silently killed the
    // flag — the advanced fixture was then checked against the basic surface, so nothing ever
    // verified that it covers Then, BestOf and OnlyIf.
    surfaceFile.convention(
        providers.gradleProperty("specSurface")
            .orElse("../spec-test-driven/exercises/write-spec/README.md"),
    )
}

tasks.register<duckshop.GenerateMutantsTask>("generatePricingMutants") {
    group = "duck-shop"
    description = "Regenerate the 11.4 pricing mutant modules from pricing-mutants/catalog.json."
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    packagePath.set("org/jetbrains/kotlin/course/duck/shop/pricing")
    // The mutated file is the teacher-only reference, not :core.
    coreSrc.set("reference/src/main/kotlin")
    // :core is compiled in unmutated — the reference needs Duck, DiscountRule and AdmissionPolicy.
    alsoCompile.set("../spec-test-driven/core/src/main/kotlin")
    // These mutants exist to be shot at by the property catalog, not by the 11.2 admission suite.
    defaultTests.set("pricing-properties/kotlin")
}

tasks.register<duckshop.MutationReportTask>("verifyPricingMutants") {
    group = "verification"
    description = "Run a property suite against every 11.4 pricing mutant. " +
        "Params: [-PmutantTests=<test source dir>] [-PmutantsStrict]."
    catalogPath.set("pricing-mutants/catalog.json")
    defaultTests.set("pricing-properties/kotlin")
    outPath.set("pricing-mutants")
    dependsOn(subprojects.filter { it.path.startsWith(":pricing-mutants:") }.map { "${it.path}:test" })
}

tasks.register<duckshop.PropertyMatrixTask>("pricingPropertyMatrix") {
    group = "verification"
    description = "Which property kills which pricing mutant — the definition of load-bearing."
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    dependsOn("verifyPricingMutants")
}

// The appeal against layer 2: score the claims a learner declares for themselves, rather than the
// ones a model read out of their prose. Everything past the declaration is deterministic.
tasks.register<duckshop.ClaimReportTask>("verifyClaims") {
    group = "verification"
    description = "Run the claims you declare as properties, and show where they disagree with the " +
        "extraction. Params: -Pclaims=<ids or names> | claims.txt beside -Pspec, [-Pagent=<run>]."
    propertyFile.set(
        "pricing-properties/kotlin/org/jetbrains/kotlin/course/duck/shop/pricing/PricingProperties.kt",
    )
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    dependsOn("verifyPricingMutants")
}

// 11.4 step 3e: what an agent builds when the specification is all it has.
tasks.register<duckshop.ImplementationReportTask>("verifyImplementations") {
    group = "verification"
    description = "Report where implementations written from a specification alone diverge from the " +
        "reference, tier-aware. Params: -Pagent=<name> [-Pkey=<key.json>]."
    keyPath.convention(providers.gradleProperty("key").orElse("fixtures/11.4/key.json"))
    dependsOn(
        subprojects.filter { it.path.startsWith(":implementations:") && it.buildFile.exists() }
            .map { "${it.path}:test" },
    )
}

// The interactive route into 3e: a frontier agent driven by hand in another window.
tasks.register<duckshop.PrepareImplementationTask>("prepareImplementation") {
    group = "duck-shop"
    description = "Scaffold a hand-placed implementation so verifyImplementations can score it, or " +
        "print the prompt to hand the agent. Params: -Pagent=<name> -Pspec=<specification>."
}

// How much did each specification leave to chance? Two independent implementations of the same text,
// compared with each other and with the reference.
tasks.register<duckshop.DivergenceReportTask>("verifyDivergence") {
    group = "verification"
    description = "Compare two independent implementations of the same specification against each " +
        "other and the reference. Params: -PagentA=<run> -PagentB=<run> [-Pspec=<one>] [-Pexamples=n]."
    referenceRecording.convention("reference-probe/recording.txt")
    dependsOn(":reference-probe:test")
}
