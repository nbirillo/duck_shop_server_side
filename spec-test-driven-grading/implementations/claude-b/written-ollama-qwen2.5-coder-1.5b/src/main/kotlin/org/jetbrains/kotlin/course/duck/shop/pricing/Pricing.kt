package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

// Implement these from SPEC.md. Where the specification does not settle something, decide
// and move on — a gap is not a reason to stop, and what you decide is exactly what the
// exercise is looking at.
//
// DiscountRule may have cases SPEC.md never describes. Leave the price unchanged for those:
// your `when` still has to be exhaustive.

/**
 * Returns the price of [duck] with [rules] applied.
 *
 * Decisions taken where SPEC.md leaves the answer open:
 * - Rules are applied **sequentially**, in list order: each rule sees the price the previous
 *   one produced (SPEC.md §5 asks sequential-or-simultaneous; §4 says order is respected).
 * - `Percentage` truncates the *discount*, so the surviving price rounds up: 10% off 55 is
 *   a discount of 5, giving 50. Both of §2's examples are exact, so they are unaffected.
 * - `BigSpenderBonus` honours its threshold — a price below it is left alone (SPEC.md §3).
 * - The price is clamped at 0 and computed in `Long`, so it never goes negative and
 *   `Int.MAX_VALUE` inputs do not overflow (SPEC.md §3).
 * - `Then`, `BestOf` and `OnlyIf` are never described by SPEC.md, so they leave the price
 *   unchanged.
 */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule -> apply(rule, price) }

private fun apply(rule: DiscountRule, price: Int): Int = when (rule) {
    is DiscountRule.Percentage -> reduceBy(price, price.toLong() * rule.percent / 100)
    is DiscountRule.AmountOff -> reduceBy(price, rule.amount.toLong())
    is DiscountRule.BigSpenderBonus ->
        if (price >= rule.threshold) reduceBy(price, rule.amount.toLong()) else price

    // Not described by SPEC.md: leave the price unchanged.
    is DiscountRule.Then -> price
    is DiscountRule.BestOf -> price
    is DiscountRule.OnlyIf -> price
}

/** Subtracts [discount] from [price], never dropping below 0 and never overflowing. */
private fun reduceBy(price: Int, discount: Long): Int =
    (price - discount).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
