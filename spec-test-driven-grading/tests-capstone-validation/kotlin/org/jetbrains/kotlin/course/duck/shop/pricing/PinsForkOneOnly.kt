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
 * ### Why there are two tests, which is now history rather than necessity
 *
 * When fork 1 still held a third reading — *the chain overrides* — the first test alone reported LEFT
 * OPEN, and rightly: "the chain refused, so nobody sells" is answered identically by *both must admit*
 * and by *the chain overrides*, so it only ruled out *the shop decides alone*. **One counterexample
 * closes one alternative, not a fork.** That lesson is worth showing a learner verbatim, and it is the
 * same shape as hardening against a counterexample closing the instance and not the class.
 *
 * That third reading was later removed — `verifyForks` showed it rejecting a *settled* fact, because it
 * made every shop's own admission rule inert — so either test now settles fork 1 on its own. Both are
 * kept: the second is still a correct and useful example, and deleting it would delete the story.
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
