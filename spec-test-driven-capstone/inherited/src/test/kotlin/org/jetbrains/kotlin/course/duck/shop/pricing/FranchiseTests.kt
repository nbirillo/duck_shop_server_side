package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.AllOf
import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.KotlinOnly
import org.jetbrains.kotlin.course.duck.shop.admission.MaxBudget
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for the franchise feature. These came with the implementation.
 */
class FranchiseTests {

    private val anyDuck = AllOf(emptyList())

    private fun duck(price: Int) = Duck("Ducky", price, true, emptyList())

    @Test
    fun `returns an offer from the only shop in the chain`() {
        val shop = Shop("Central", anyDuck)
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(shop))

        val offer = assertNotNull(bestOffer(duck(100), franchise))

        assertEquals("Central", offer.shop.name)
        assertEquals(100, offer.price)
    }

    @Test
    fun `returns the cheaper of two shops`() {
        val expensive = Shop("Central", anyDuck)
        val cheap = Shop("Riverside", anyDuck, listOf(DiscountRule.Percentage(20)))
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(expensive, cheap))

        val offer = assertNotNull(bestOffer(duck(100), franchise))

        assertEquals("Riverside", offer.shop.name)
        assertEquals(80, offer.price)
    }

    @Test
    fun `returns the cheaper shop regardless of the order they are listed in`() {
        val cheap = Shop("Riverside", anyDuck, listOf(DiscountRule.Percentage(20)))
        val expensive = Shop("Central", anyDuck)
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(cheap, expensive))

        val offer = assertNotNull(bestOffer(duck(100), franchise))

        assertEquals("Riverside", offer.shop.name)
    }

    @Test
    fun `applies a shop's promotion to the price`() {
        val shop = Shop("Central", anyDuck, listOf(DiscountRule.AmountOff(15)))
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(shop))

        assertEquals(85, assertNotNull(bestOffer(duck(100), franchise)).price)
    }

    @Test
    fun `applies two of a shop's promotions in order`() {
        val shop = Shop(
            "Central",
            anyDuck,
            listOf(DiscountRule.Percentage(50), DiscountRule.AmountOff(10)),
        )
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(shop))

        assertEquals(40, assertNotNull(bestOffer(duck(100), franchise)).price)
    }

    @Test
    fun `no offer when no shop will take the duck`() {
        val tooExpensive = Shop("Central", MaxBudget(50))
        val alsoTooExpensive = Shop("Riverside", MaxBudget(70))
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(tooExpensive, alsoTooExpensive))

        assertNull(bestOffer(duck(100), franchise))
    }

    @Test
    fun `a shop that will not take the duck is skipped`() {
        val fussy = Shop("Central", MaxBudget(50), listOf(DiscountRule.Percentage(90)))
        val ordinary = Shop("Riverside", anyDuck)
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(fussy, ordinary))

        val offer = assertNotNull(bestOffer(duck(100), franchise))

        assertEquals("Riverside", offer.shop.name)
    }

    @Test
    fun `a Kotlin duck is admitted by a Kotlin-only shop`() {
        val shop = Shop("Central", KotlinOnly())
        val franchise = Franchise("Duck Co", anyDuck, emptyList(), listOf(shop))

        assertEquals(100, assertNotNull(bestOffer(duck(100), franchise)).price)
    }
}
