package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * My claims about `priceFor`, as things that can fail.
 *
 * One test per claim, and the test name IS the claim — so a red test reads as a sentence about the
 * implementation rather than as "assertion failed at line 41".
 *
 * Every claim is stated over GENERATED inputs, not chosen ones. A claim is about every input, and an
 * example cannot say that: `priceFor(100, [Percentage(10)]) == 90` is one fact, while "no rule ever
 * raises the price" is the thing I actually believe.
 *
 * The seed is fixed so a failure I see is a failure you see.
 */
class PriceProperties {

    private val random = Random(SEED)

    private fun ducks() = List(CASES) {
        Duck(name = "duck-$it", price = random.nextInt(0, 5_000), hasKotlinAttribute = random.nextBoolean())
    }

    @Test
    fun `an empty rule list leaves the price alone`() {
        ducks().forEach { duck ->
            assertEquals(duck.price, priceFor(duck, emptyList()), duck.name)
        }
    }

    @Test
    fun `no rule ever raises the price`() {
        ducks().forEach { duck ->
            val rules = listOf(
                DiscountRule.Percentage(random.nextInt(0, 101)),
                DiscountRule.AmountOff(random.nextInt(0, 500)),
                DiscountRule.BigSpenderBonus(threshold = random.nextInt(0, 3_000), amount = random.nextInt(0, 200)),
            )
            val out = priceFor(duck, rules)
            assertTrue(out <= duck.price, "${duck.name}: $out > ${duck.price}")
        }
    }

    // A claim worth adding once you have DECIDED it — and only once you have. Whether the price may
    // go below zero is not in the brief, so writing this test is me settling it, and my specification
    // had better say so too. Delete the comment when the spec does.
    //
    // @Test
    // fun `the price is never negative`() {
    //     ducks().forEach { duck ->
    //         val rules = listOf(DiscountRule.AmountOff(random.nextInt(0, 10_000)))
    //         assertTrue(priceFor(duck, rules) >= 0, "${duck.name} went negative")
    //     }
    // }

    private companion object {
        const val SEED = 1L
        const val CASES = 200
    }
}
