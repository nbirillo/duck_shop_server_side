package org.jetbrains.kotlin.course.duck.shop.admission

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 11.2 — write tests for the admission-policy algebra (given and correct in `:core`).
 *
 * Your job here is NOT to implement anything — the policies already work. Your job is to write
 * tests that pin their behaviour and would catch a wrong implementation. Think about the corner
 * cases where an implementation (human or AI) tends to slip.
 *
 * Run: `./gradlew :exercises:write-tests:test`
 */
class PolicyTests {

    private val plainDuck = Duck(name = "Donald", price = 40, hasKotlinAttribute = false)

    @Test
    fun `MaxBudget admits a duck priced below the limit`() {
        assertTrue(MaxBudget(50).admits(plainDuck))
    }

    // TODO: add your own tests. Questions worth pinning down with a test:
    //  - MaxBudget exactly at the limit — is the boundary inclusive or exclusive?
    //  - RequiresAccessory when the duck has no accessories at all.
    //  - MinAccessories at exactly the minimum.
    //  - AllOf and AnyOf over an EMPTY list — what should each return, and why?
    //  - Not — does it truly invert?
}
