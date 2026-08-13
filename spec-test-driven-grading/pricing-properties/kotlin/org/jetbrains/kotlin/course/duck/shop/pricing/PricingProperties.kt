package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.KotlinOnly
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The authored property catalog for 11.4 — the base set, and the load-bearing layer of the check.
 * **No model is involved here.** One test per claim a specification might make, each stated over
 * generated inputs rather than examples, because the point of a property is that it holds for
 * *every* input and examples cannot say that.
 *
 * These run against the reference and against every mutant. Two rules give the whole layer its
 * meaning:
 *
 *  - a property that **fails on the reference** is simply wrong;
 *  - a property that **kills no mutant** pins nothing, however true it is — `pricingPropertyMatrix`
 *    reports those, because a specification full of them reads as rigour and constrains nobody.
 *
 * The teacher can regenerate this set with a model of their choosing, and the student build has an
 * opt-in mode for adding to it; see `teacher/exercise-11.4-answer-key.md`. This is the default.
 *
 * Test names are the property ids: layer 2 maps a learner's claims onto these names, so renaming one
 * renames it everywhere.
 */
class PricingProperties {

    private val random = Random(SEED)

    private fun ducks() = List(CASES) {
        Duck(
            name = "duck-$it",
            price = random.nextInt(0, 5_000),
            hasKotlinAttribute = random.nextBoolean(),
        )
    }

    private fun percent() = random.nextInt(0, 101)

    // The next two are KEPT ON PURPOSE even though the matrix reports that they kill nothing. They
    // are perfectly true and constrain no implementation in the catalog, which makes them the live
    // example of the distinction the whole layer exists to draw: true and load-bearing are not the
    // same thing. Do not "fix" them by inventing a mutant that only they catch.
    @Test
    fun `an empty rule list leaves the price alone`() {
        ducks().forEach { assertEquals(it.price, priceFor(it, emptyList()), it.name) }
    }

    @Test
    fun `no rule ever raises the price`() {
        ducks().forEach { duck ->
            val rules = listOf(DiscountRule.Percentage(percent()), DiscountRule.AmountOff(random.nextInt(0, 500)))
            assertTrue(priceFor(duck, rules) <= duck.price, "${duck.name}: ${priceFor(duck, rules)} > ${duck.price}")
        }
    }

    @Test
    fun `the price is never negative`() {
        ducks().forEach { duck ->
            val rules = listOf(DiscountRule.AmountOff(random.nextInt(0, 10_000)))
            assertTrue(priceFor(duck, rules) >= 0, "${duck.name} went negative")
        }
    }

    @Test
    fun `a percentage subtracts a rounded-down discount`() {
        ducks().forEach { duck ->
            val p = percent()
            val expected = duck.price - (duck.price.toLong() * p / 100).toInt()
            assertEquals(expected, priceFor(duck, listOf(DiscountRule.Percentage(p))), "${duck.name} at $p%")
        }
    }

    @Test
    fun `rules compound — each sees what the one before it left`() {
        ducks().forEach { duck ->
            val a = percent()
            val b = percent()
            val once = priceFor(duck, listOf(DiscountRule.Percentage(a)))
            val twice = priceFor(duck, listOf(DiscountRule.Percentage(a), DiscountRule.Percentage(b)))
            val expected = once - (once.toLong() * b / 100).toInt()
            assertEquals(expected, twice, "${duck.name} at $a% then $b%")
        }
    }

    @Test
    fun `the big-spender threshold is inclusive`() {
        ducks().forEach { duck ->
            val bonus = DiscountRule.BigSpenderBonus(threshold = duck.price, amount = 1)
            assertEquals(
                (duck.price - 1).coerceAtLeast(0),
                priceFor(duck, listOf(bonus)),
                "${duck.name} priced exactly at the threshold",
            )
        }
    }

    @Test
    fun `the big-spender bonus reads the price it is applied to`() {
        // A shelf price at or over the threshold, cut to under it by an earlier rule. The discount has
        // to be PARTIAL: take everything off and the running price is 0, where the floor swallows the
        // bonus and both readings agree — the first version of this property did exactly that and let
        // the mutant live.
        ducks().filter { it.price in 100..199 }.forEach { duck ->
            val rules = listOf(DiscountRule.Percentage(50), DiscountRule.BigSpenderBonus(100, 20))
            val halved = priceFor(duck, listOf(DiscountRule.Percentage(50)))
            assertEquals(halved, priceFor(duck, rules), "${duck.name}: the bonus fired on $halved, under the threshold")
        }
    }

    @Test
    fun `a large price does not overflow`() {
        // TOLERANCE OF 1 ON PURPOSE — do not "tighten" this back to assertEquals.
        //
        // It used to demand the exact value, which pins the rounding DIRECTION as a side effect, so
        // this property also killed round-the-remainder and round-up. Both differ from the ideal by
        // exactly 1 here, and neither has anything to do with 32-bit arithmetic. The consequence was
        // a scoring distortion we measured: a specification that says nothing about rounding was
        // credited with pinning it, and one fixture that mentions only overflow tied a spec four
        // times its equal. A property should kill what its claim rules out, and nothing else.
        //
        // Within 1 the two rounding mutants survive here (claims 4 and 5 own them) while an Int
        // multiplication still wraps to a result that is out by millions.
        listOf(Int.MAX_VALUE, Int.MAX_VALUE - 1, 2_000_000_000).forEach { price ->
            val duck = Duck("big", price, hasKotlinAttribute = false)
            val ideal = price - (price.toLong() * 10 / 100).toInt()
            val actual = priceFor(duck, listOf(DiscountRule.Percentage(10)))
            assertTrue(
                actual in (ideal - 1)..(ideal + 1),
                "price $price: got $actual, and exact arithmetic gives $ideal — that is not a rounding difference",
            )
        }
    }

    @Test
    fun `BestOf hands over the lowest of its options`() {
        ducks().forEach { duck ->
            val options = List(3) { DiscountRule.Percentage(percent()) }
            val best = options.minOf { priceFor(duck, listOf(it)) }
            // The LIST form on purpose, even for a single rule: the basic-tier brief only ever
            // shows priceFor(duck, List<DiscountRule>), so an implementation written from a basic
            // specification has no single-rule overload and this file would not compile against it.
            assertEquals(best, priceFor(duck, listOf(DiscountRule.BestOf(options))), duck.name)
        }
    }

    @Test
    fun `OnlyIf leaves a duck its condition rejects untouched`() {
        ducks().forEach { duck ->
            val rule = DiscountRule.OnlyIf(KotlinOnly(), DiscountRule.AmountOff(random.nextInt(1, 500)))
            val expected = if (duck.hasKotlinAttribute) priceFor(duck, listOf(rule.rule)) else duck.price
            assertEquals(expected, priceFor(duck, listOf(rule)), duck.name)
        }
    }

    private companion object {
        const val SEED = 20260813L
        const val CASES = 300
    }
}
