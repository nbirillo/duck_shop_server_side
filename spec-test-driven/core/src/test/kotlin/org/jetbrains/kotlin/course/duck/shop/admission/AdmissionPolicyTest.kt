package org.jetbrains.kotlin.course.duck.shop.admission

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test-first specification for [AdmissionPolicy] and its leaves/combinators.
 *
 * These tests are written BEFORE the implementations (which are currently `TODO()` stubs),
 * so the whole suite is expected to fail ("red") until an implementation is provided. They
 * deliberately pin down the corner cases where implementations — human or AI — tend to slip:
 * the `<=` boundary, empty accessories, and the vacuous-truth results of `All`/`Any`.
 */
class AdmissionPolicyTest {

    private val plainDuck = Duck(name = "Donald", price = 40, hasKotlinAttribute = false)
    private val kotlinDuck = Duck(name = "Kotlina", price = 40, hasKotlinAttribute = true)
    private val fancyDuck = Duck(
        name = "Scrooge",
        price = 100,
        hasKotlinAttribute = true,
        accessories = listOf(Accessory("hat"), Accessory("monocle")),
    )

    // ---- KotlinOnly ----

    @Test
    fun `KotlinOnly admits a duck with the Kotlin attribute and rejects one without`() {
        assertTrue(KotlinOnly().admits(kotlinDuck))
        assertFalse(KotlinOnly().admits(plainDuck))
    }

    // ---- MaxBudget (boundary: <= not <) ----

    @Test
    fun `MaxBudget admits a duck priced below the limit`() {
        assertTrue(MaxBudget(50).admits(plainDuck)) // 40 <= 50
    }

    @Test
    fun `MaxBudget admits a duck priced exactly at the limit`() {
        assertTrue(MaxBudget(40).admits(plainDuck)) // boundary: 40 <= 40
    }

    @Test
    fun `MaxBudget rejects a duck priced above the limit`() {
        assertFalse(MaxBudget(39).admits(plainDuck)) // 40 > 39
    }

    // ---- RequiresAccessory (empty accessories) ----

    @Test
    fun `RequiresAccessory admits a duck wearing the accessory and rejects one that is not`() {
        assertTrue(RequiresAccessory("hat").admits(fancyDuck))
        assertFalse(RequiresAccessory("scarf").admits(fancyDuck))
    }

    @Test
    fun `RequiresAccessory rejects a duck with no accessories`() {
        assertFalse(RequiresAccessory("hat").admits(plainDuck)) // accessories = []
    }

    // ---- MinAccessories ----

    @Test
    fun `MinAccessories checks the count against the minimum`() {
        assertTrue(MinAccessories(2).admits(fancyDuck))  // has 2
        assertTrue(MinAccessories(0).admits(plainDuck))  // 0 >= 0
        assertFalse(MinAccessories(3).admits(fancyDuck)) // 2 < 3
    }

    // ---- All (AND + vacuous truth) ----

    @Test
    fun `All admits only when every policy admits`() {
        assertTrue(All(KotlinOnly(), MaxBudget(50)).admits(kotlinDuck))
        assertFalse(All(KotlinOnly(), MaxBudget(50)).admits(plainDuck)) // fails KotlinOnly
    }

    @Test
    fun `All of no policies admits every duck (vacuous truth)`() {
        assertTrue(All(emptyList()).admits(plainDuck))
    }

    // ---- Any (OR + vacuous truth) ----

    @Test
    fun `Any admits when at least one policy admits`() {
        assertTrue(Any(KotlinOnly(), MaxBudget(10)).admits(kotlinDuck)) // passes KotlinOnly
        assertFalse(Any(KotlinOnly(), RequiresAccessory("hat")).admits(plainDuck)) // passes neither
    }

    @Test
    fun `Any of no policies admits no duck (vacuous truth)`() {
        assertFalse(Any(emptyList()).admits(kotlinDuck))
    }

    // ---- Not ----

    @Test
    fun `Not inverts the wrapped policy`() {
        assertTrue(Not(KotlinOnly()).admits(plainDuck))
        assertFalse(Not(KotlinOnly()).admits(kotlinDuck))
    }
}
