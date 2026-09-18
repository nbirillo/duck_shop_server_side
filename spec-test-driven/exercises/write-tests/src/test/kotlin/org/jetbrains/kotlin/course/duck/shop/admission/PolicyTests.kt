package org.jetbrains.kotlin.course.duck.shop.admission

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * VERIFY AND HARDEN an AI-generated test suite.
 *
 * An AI was asked to test the admission-policy algebra (given and correct in `:core`). Below is
 * what it produced. Your job is NOT to implement anything — it is to VERIFY these tests:
 *
 *   1. One of these tests is WRONG — it asserts the wrong thing and fails against the correct
 *      code. Find it and fix it so the suite is green (`./gradlew :exercises:write-tests:test`).
 *   2. The suite MISSES important cases. Add tests for them (see README.md).
 *   3. Two behaviours are left unspecified (see README.md). Decide what they should be and pin
 *      your decision with a test — or flag the ambiguity.
 *
 * Then check your suite actually catches bugs: `./gradlew verifyMutants --continue`.
 */
class PolicyTests {

    private val plainDuck = Duck(name = "Donald", price = 40, hasKotlinAttribute = false)
    private val kotlinDuck = Duck(name = "Kotlina", price = 40, hasKotlinAttribute = true)
    private val hatDuck =
        Duck(name = "Scrooge", price = 100, hasKotlinAttribute = true, accessories = listOf(Accessory("hat")))

    @Test
    fun `KotlinOnly admits a duck with the Kotlin attribute`() {
        assertTrue(KotlinOnly().admits(kotlinDuck))
    }

    @Test
    fun `KotlinOnly rejects a duck without the Kotlin attribute`() {
        assertFalse(KotlinOnly().admits(plainDuck))
    }

    @Test
    fun `MaxBudget admits a duck priced below the limit`() {
        assertTrue(MaxBudget(50).admits(plainDuck))
    }

    @Test
    fun `MaxBudget rejects a duck priced above the limit`() {
        assertFalse(MaxBudget(30).admits(plainDuck))
    }

    @Test
    fun `RequiresAccessory admits a duck wearing the accessory`() {
        assertTrue(RequiresAccessory("hat").admits(hatDuck))
    }

    @Test
    fun `RequiresAccessory rejects a duck wearing only a different accessory`() {
        val scarfDuck = Duck("Daisy", 20, hasKotlinAttribute = false, accessories = listOf(Accessory("scarf")))
        assertFalse(RequiresAccessory("hat").admits(scarfDuck))
    }

    @Test
    fun `MinAccessories admits a duck with enough accessories`() {
        assertTrue(MinAccessories(1).admits(hatDuck))
    }

    @Test
    fun `AllOf admits a duck when all policies admit`() {
        assertTrue(AllOf(KotlinOnly(), MaxBudget(200)).admits(hatDuck))
    }

    @Test
    fun `AnyOf admits a duck when at least one policy admits`() {
        assertTrue(AnyOf(KotlinOnly(), MaxBudget(10)).admits(kotlinDuck))
    }

    @Test
    fun `Not returns the decision of the wrapped policy`() {
        // The AI wrote this one. Not is supposed to INVERT the wrapped policy — is this assertion right?
        assertTrue(Not(KotlinOnly()).admits(kotlinDuck))
    }
}
