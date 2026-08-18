package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.AllOf
import org.jetbrains.kotlin.course.duck.shop.admission.AnyOf
import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * HARNESS SELF-TEST, not part of any exercise — the same role `attacks/demo-prefix/` plays for the
 * adversary check and `solutions/demo-buggy/` for the mutant engine.
 *
 * It decides **exactly one** fork: the chain's rule is a filter. It says nothing about promotion
 * order, nothing about when cheapest is measured, nothing about an empty chain. So `verifyForks` has
 * to report fork 1 SETTLED and the other three LEFT OPEN — and if it ever reports all four settled,
 * the check is passing readings it never actually ran.
 *
 * ### It takes TWO tests to settle a fork with three readings, and finding that out is the point
 *
 * The first test alone reported fork 1 as LEFT OPEN, which was right: "the chain refused, so nobody
 * sells" is answered the same way by *both must admit* and by *the chain overrides* — it only rules out
 * *the shop decides alone*. Closing a fork means rejecting **every** reading but one, so a second test
 * has to catch a duck the chain admits and a shop does not. Worth showing a learner verbatim: one
 * counterexample closes one alternative, not a fork.
 */
class PinsForkOneOnly {

    private val open: AdmissionPolicy = AllOf(emptyList())
    private val closed: AdmissionPolicy = AnyOf(emptyList())
    private val duck = Duck("d", 100, true, emptyList())

    @Test
    fun `the chain's own rule can refuse a duck every shop would sell`() {
        val franchise = Franchise("f", closed, emptyList(), listOf(Shop("s0", open), Shop("s1", open)))
        assertNull(bestOffer(duck, franchise))
    }

    @Test
    fun `a shop's own rule still refuses a duck the chain admits`() {
        // s0 is the cheaper shop but will not have this duck. Under "both must admit" it drops out and
        // the answer is s1 at full price; under "the chain overrides" its rule is ignored and s0 wins
        // at 50. That difference is what separates the two readings the first test could not.
        val franchise = Franchise(
            "f",
            open,
            emptyList(),
            listOf(Shop("s0", closed, listOf(DiscountRule.Percentage(50))), Shop("s1", open)),
        )
        val offer = assertNotNull(bestOffer(duck, franchise), "s1 admits this duck")
        assertEquals("s1", offer.shop.name, "s0 refuses this duck, so its promotion is not on offer")
        assertEquals(100, offer.price)
    }
}
