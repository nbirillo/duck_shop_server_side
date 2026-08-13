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
    // this build sits one level over, so the surface file is across the sibling
    surfaceFile.convention("../spec-test-driven/exercises/write-spec/README.md")
}

tasks.register<duckshop.GenerateMutantsTask>("generatePricingMutants") {
    group = "duck-shop"
    description = "Regenerate the 11.4 pricing mutant modules from pricing-mutants/catalog.json."
    catalogPath.set("pricing-mutants/catalog.json")
    outPath.set("pricing-mutants")
    packagePath.set("org/jetbrains/kotlin/course/duck/shop/pricing")
    // :core is compiled in unmutated — the reference needs Duck, DiscountRule and AdmissionPolicy.
    alsoCompile.set("../spec-test-driven/core/src/main/kotlin")
}

tasks.register<duckshop.MutationReportTask>("verifyPricingMutants") {
    group = "verification"
    description = "Run a property suite against every 11.4 pricing mutant. " +
        "Params: [-PmutantTests=<test source dir>] [-PmutantsStrict]."
    catalogPath.set("pricing-mutants/catalog.json")
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
