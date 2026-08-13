package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.KotlinOnly
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Not a property suite — that comes later. This only pins the handful of numbers the reference's own
 * documentation claims, so a mistake in the reference is caught before any learner is measured
 * against it.
 */
class ReferenceSanityTest {
    private fun duck(price: Int, kotlin: Boolean = false) =
        Duck(name = "d", price = price, hasKotlinAttribute = kotlin)

    @Test
    fun `the discount is rounded down, not the remainder`() {
        assertEquals(86, priceFor(duck(95), listOf(DiscountRule.Percentage(10))))
    }

    @Test
    fun `a large price does not overflow`() {
        assertEquals(1932735283, priceFor(duck(Int.MAX_VALUE), listOf(DiscountRule.Percentage(10))))
    }

    @Test
    fun `the bonus reads the price it is applied to`() {
        val rules = listOf(DiscountRule.Percentage(10), DiscountRule.BigSpenderBonus(100, 20))
        assertEquals(90, priceFor(duck(100), rules))
    }

    @Test
    fun `the threshold is inclusive`() {
        assertEquals(80, priceFor(duck(100), listOf(DiscountRule.BigSpenderBonus(100, 20))))
    }

    @Test
    fun `the price never goes below zero`() {
        assertEquals(0, priceFor(duck(10), listOf(DiscountRule.AmountOff(999))))
    }

    @Test
    fun `BestOf gives the customer the better deal`() {
        val rule = DiscountRule.BestOf(listOf(DiscountRule.AmountOff(1), DiscountRule.Percentage(50)))
        assertEquals(50, priceFor(duck(100), rule))
    }

    @Test
    fun `OnlyIf skips a duck its condition rejects`() {
        val rule = DiscountRule.OnlyIf(KotlinOnly(), DiscountRule.AmountOff(10))
        assertEquals(100, priceFor(duck(100, kotlin = false), rule))
        assertEquals(90, priceFor(duck(100, kotlin = true), rule))
    }
}
