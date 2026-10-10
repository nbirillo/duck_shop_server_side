// Root of the capstone build. It carries no answer key: the graded fork readings, the reference and
// the property catalogs all live in spec-test-driven-grading/, which is not handed out.

plugins {
    kotlin("jvm") version "2.2.20" apply false
    id("duck-shop.run-agent")
}

// The capstone applies only duck-shop.run-agent, so verifyDivergence does not exist here. Its next
// step is the one its README describes: replace the inherited implementation, then run YOUR tests.
tasks.withType<duckshop.RunAgentTask>().configureEach {
    implNextStep.set(
        "put it in place of inherited/src/main/kotlin/org/jetbrains/kotlin/course/duck/shop/" +
            "pricing/BestOffer.kt, then ./gradlew :work:test   (your tests against it)",
    )
}
