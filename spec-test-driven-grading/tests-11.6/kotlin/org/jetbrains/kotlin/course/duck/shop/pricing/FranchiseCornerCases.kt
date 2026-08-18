package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Accessory
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.AllOf
import org.jetbrains.kotlin.course.duck.shop.admission.AnyOf
import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TEACHER-ONLY. The key corner cases of the 11.6 capstone, run against one implementation of
 * `bestOffer` at a time (`-PimplTests=tests-11.6/kotlin`).
 *
 * ### The split is the whole design of this file
 *
 * `briefs/11.6-franchise.md` divides the feature into what it **settles** and what it **leaves open**,
 * and a hidden set that ignores that division punishes a learner for making a legitimate choice. So:
 *
 *  - **`settled — …`** follows from the brief's own settled list and from nothing else. A red one here
 *    is a defect on any reading. **This group, and only this group, is the failure profile.**
 *  - **`open — …`** pins a fork to `reference/BestOffer.kt`, which is *a choice, not a truth*. A red
 *    one means "this implementation read the brief differently", which is information, not a fault.
 *    Score it separately and never sum the two.
 *
 * The same distinction the mutant catalog draws between `must-kill` and `spec-dependent`, one level up.
 *
 * ### One honest tension, flagged rather than resolved
 *
 * The brief lists "the chain admits and no shop does, or the reverse" as open (fork 5) while also
 * settling "a duck nobody will sell has no answer at all". Those overlap: if no shop will sell, the
 * second line decides it whatever the first says. `settled — a duck no shop admits has no answer` is
 * placed in the settled group on the strength of that line. If a run shows agents reading it otherwise,
 * the brief is what needs fixing — not this file.
 */
class FranchiseCornerCases {

    // ── settled: true on every reading of the brief ───────────────────────────────────────────────

    @Test
    fun `settled — cheapest wins, not the first shop that would sell`() {
        val franchise = chain(shop("s0"), shop("s1", promos = listOf(DiscountRule.Percentage(50))))
        val offer = assertNotNull(bestOffer(duck(100), franchise), "some shop admits this duck")
        assertEquals("s1", offer.shop.name, "s1 is cheaper; naming s0 means 'first', not 'cheapest'")
        assertEquals(50, offer.price)
    }

    @Test
    fun `settled — cheapest wins, not the last shop either`() {
        val franchise = chain(shop("s0", promos = listOf(DiscountRule.Percentage(50))), shop("s1"))
        val offer = assertNotNull(bestOffer(duck(100), franchise))
        assertEquals("s0", offer.shop.name, "naming s1 means 'last', not 'cheapest'")
        assertEquals(50, offer.price)
    }

    @Test
    fun `settled — the shop named is one of the chain's own shops`() {
        val shops = listOf(shop("s0", promos = listOf(DiscountRule.AmountOff(7))), shop("s1"), shop("s2"))
        val offer = assertNotNull(bestOffer(duck(100), chain(*shops.toTypedArray())))
        assertTrue(offer.shop.name in shops.map { it.name }, "named ${offer.shop.name}, which is not in the chain")
    }

    @Test
    fun `settled — a shop's own promotions reach the price`() {
        // Whatever a reader decides about combining the two sets, a shop's promotions cannot be ignored:
        // the brief settles that each shop keeps the promotions it already runs.
        val franchise = chain(shop("s0", promos = listOf(DiscountRule.Percentage(50))))
        assertEquals(50, assertNotNull(bestOffer(duck(100), franchise)).price)
    }

    @Test
    fun `settled — the chain's own promotions reach the price`() {
        // Same argument in the other direction: "the chain has rules of its own on top of that".
        // Every open reading of fork 2 — chain's, both, the better of the two — gives 50 here, because
        // the one shop runs no promotion of its own. Only ignoring the chain's promotions gives 100.
        val franchise = Franchise("f", OPEN, listOf(DiscountRule.Percentage(50)), listOf(shop("s0")))
        assertEquals(50, assertNotNull(bestOffer(duck(100), franchise)).price)
    }

    @Test
    fun `settled — a duck no shop admits has no answer`() {
        val franchise = Franchise("f", OPEN, emptyList(), listOf(shop("s0", CLOSED), shop("s1", CLOSED)))
        assertNull(bestOffer(duck(100), franchise), "nobody will sell it, so there is nothing to answer")
    }

    @Test
    fun `settled — an offer never costs more than the duck does on the shelf`() {
        // Inherited from 11.4: a promotion is a discount. Checked here because a franchise makes it
        // newly easy to break — adding two discounts the wrong way round can raise a price.
        val franchise = Franchise(
            "f",
            OPEN,
            listOf(DiscountRule.AmountOff(10)),
            listOf(shop("s0", promos = listOf(DiscountRule.Percentage(25)))),
        )
        listOf(0, 1, 99, 100, 5_000).forEach { price ->
            val offer = bestOffer(duck(price), franchise) ?: return@forEach
            assertTrue(offer.price <= price, "shelf $price became ${offer.price}")
            assertTrue(offer.price >= 0, "a negative price at $price")
        }
    }

    // ── open: pinned to the reference's reading, which is a choice ────────────────────────────────

    @Test
    fun `open — fork 1, the chain's rule vetoes a shop that would sell`() {
        val franchise = Franchise("f", CLOSED, emptyList(), listOf(shop("s0"), shop("s1")))
        assertNull(bestOffer(duck(100), franchise), "the reference reads the chain's rule as a filter")
    }

    @Test
    fun `open — fork 2, the shop's promotions apply before the chain's`() {
        val franchise = Franchise(
            "f",
            OPEN,
            listOf(DiscountRule.AmountOff(10)),
            listOf(shop("s0", promos = listOf(DiscountRule.Percentage(50)))),
        )
        // shop first: 100 -> 50 -> 40.   chain first: 100 -> 90 -> 45.   Order is observable.
        assertEquals(40, assertNotNull(bestOffer(duck(100), franchise)).price)
    }

    @Test
    fun `open — fork 3, a tie goes to the earliest shop in the list`() {
        assertEquals("s0", assertNotNull(bestOffer(duck(100), chain(shop("s0"), shop("s1")))).shop.name)
    }

    @Test
    fun `open — fork 3, a tie reached by different routes still goes to the earliest`() {
        val franchise = chain(
            shop("s0", promos = listOf(DiscountRule.Percentage(10))),
            shop("s1", promos = listOf(DiscountRule.AmountOff(10))),
        )
        val offer = assertNotNull(bestOffer(duck(100), franchise))
        assertEquals("s0", offer.shop.name)
        assertEquals(90, offer.price)
    }

    @Test
    fun `open — fork 4, cheapest is decided after the chain's conditional promotion`() {
        val franchise = Franchise(
            "f",
            OPEN,
            listOf(DiscountRule.BigSpenderBonus(90, 50)),
            listOf(
                shop("s0", promos = listOf(DiscountRule.Percentage(20))),
                shop("s1", promos = listOf(DiscountRule.AmountOff(5))),
            ),
        )
        // Before the chain's bonus s0 is cheaper (80 vs 95); after it s1 is (80 vs 45). Different shops.
        val offer = assertNotNull(bestOffer(duck(100), franchise))
        assertEquals("s1", offer.shop.name, "measuring cheapest before the chain's promotion names s0")
        assertEquals(45, offer.price)
    }

    @Test
    fun `open — fork 6, a chain with no shops`() {
        assertNull(bestOffer(duck(100), chain()), "the reference reads no shops as nobody to sell")
    }

    // ── fixtures ─────────────────────────────────────────────────────────────────────────────────

    private fun chain(vararg shops: Shop) = Franchise("f", OPEN, emptyList(), shops.toList())

    private fun shop(name: String, policy: AdmissionPolicy = OPEN, promos: List<DiscountRule> = emptyList()) =
        Shop(name, policy, promos)

    private fun duck(price: Int, kotlin: Boolean = true, accessories: List<Accessory> = listOf(Accessory("Hat"))) =
        Duck("d", price, kotlin, accessories)

    private companion object {
        /** Admits every duck: the identity of a conjunction. */
        val OPEN: AdmissionPolicy = AllOf(emptyList())

        /** Admits no duck: the identity of a disjunction. */
        val CLOSED: AdmissionPolicy = AnyOf(emptyList())
    }
}
