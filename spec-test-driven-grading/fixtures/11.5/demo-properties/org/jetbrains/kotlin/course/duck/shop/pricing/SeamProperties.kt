package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * A learner's suite for the seam, written to DEMONSTRATE the four cells of `verifyRefactor`.
 *
 * The first two are honest claims about the contract. The third is the interesting one: it looks like
 * diligence and is actually pinning an accident — nothing in any specification promises how many times
 * the shop's policy gets consulted, so a rewrite that asks twice is legal and this test forbids it.
 */
class SeamProperties {

    private class Spy(private val result: Boolean) : AdmissionPolicy {
        var calls = 0
        override fun admits(duck: Duck): Boolean { calls++; return result }
    }

    private fun duck(price: Int) = Duck("d", price, hasKotlinAttribute = false)

    @Test
    fun `a duck the shop refuses has no quote`() {
        assertNull(quote(duck(100), Shop("s", Spy(false)), emptyList()))
    }

    @Test
    fun `with no promotions an admitted duck costs its shelf price`() {
        assertEquals(100, quote(duck(100), Shop("s", Spy(true)), emptyList()))
    }

    @Test
    fun `the shop's policy is consulted exactly once`() {
        val spy = Spy(true)
        quote(duck(100), Shop("s", spy), emptyList())
        assertEquals(1, spy.calls, "consulted ${spy.calls} times")
    }
}
