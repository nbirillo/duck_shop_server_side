package org.jetbrains.kotlin.course.duck.shop.admission

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The single shared acceptance suite for [AdmissionPolicy], its leaves/combinators and [Shop].
 *
 * The convention plugin compiles and runs this same source against every implementation
 * module (:starter, :grading, each solutions/<agent>/). It pins down the corner cases where
 * implementations — human or AI — tend to slip: the `<=` boundary, empty accessories, and the
 * vacuous-truth results of `AllOf`/`AnyOf`. Against :starter's stubs it is red; against a
 * correct implementation it is green; against a broken one it fails on the affected case.
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

    // ---- AllOf (AND + vacuous truth) ----

    @Test
    fun `AllOf admits only when every policy admits`() {
        assertTrue(AllOf(KotlinOnly(), MaxBudget(50)).admits(kotlinDuck))
        assertFalse(AllOf(KotlinOnly(), MaxBudget(50)).admits(plainDuck)) // fails KotlinOnly
    }

    @Test
    fun `AllOf of no policies admits every duck (vacuous truth)`() {
        assertTrue(AllOf(emptyList()).admits(plainDuck))
    }

    // ---- AnyOf (OR + vacuous truth) ----

    @Test
    fun `AnyOf admits when at least one policy admits`() {
        assertTrue(AnyOf(KotlinOnly(), MaxBudget(10)).admits(kotlinDuck)) // passes KotlinOnly
        assertFalse(AnyOf(KotlinOnly(), RequiresAccessory("hat")).admits(plainDuck)) // passes neither
    }

    @Test
    fun `AnyOf of no policies admits no duck (vacuous truth)`() {
        assertFalse(AnyOf(emptyList()).admits(kotlinDuck))
    }

    // ---- Not ----

    @Test
    fun `Not inverts the wrapped policy`() {
        assertTrue(Not(KotlinOnly()).admits(plainDuck))
        assertFalse(Not(KotlinOnly()).admits(kotlinDuck))
    }

    // ---- Shop ----

    @Test
    fun `Shop admits a duck when its policy admits it and rejects it otherwise`() {
        val kotlinShop = Shop(name = "Kotlin Ducks", admissionPolicy = KotlinOnly())
        assertTrue(kotlinShop.admits(kotlinDuck))
        assertFalse(kotlinShop.admits(plainDuck))
    }

    @Test
    fun `Shop applies a composite admission policy`() {
        val boutique = Shop(
            name = "Kotlin Boutique",
            admissionPolicy = AllOf(KotlinOnly(), RequiresAccessory("hat")),
        )
        assertTrue(boutique.admits(fancyDuck))   // kotlin + wears a hat
        assertFalse(boutique.admits(kotlinDuck)) // kotlin but no accessories
    }
}
