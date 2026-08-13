package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

// Implement these from SPEC.md. Where the specification does not settle something, decide
// and move on — a gap is not a reason to stop, and what you decide is exactly what the
// exercise is looking at.
//
// DiscountRule may have cases SPEC.md never describes. Leave the price unchanged for those:
// your `when` still has to be exhaustive.

/**
 * Returns the final price of [duck] after applying [rules] to its shelf price.
 *
 * Rules are applied in list order, each to the price left by the rules before it (SPEC §2).
 * An empty list returns the shelf price unchanged (SPEC §3).
 */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule -> apply(rule, price) }

private fun apply(rule: DiscountRule, price: Int): Int = when (rule) {
    // SPEC §2: reduce by `percent` percent of the price it is applied to, rounding down.
    // SPEC §5: a non-positive percent is ignored.
    is DiscountRule.Percentage ->
        if (rule.percent <= 0) price else price - floorDiv(price * rule.percent, 100)

    // SPEC §2: reduce by a fixed `amount`.
    // SPEC §5: a non-positive amount is ignored.
    is DiscountRule.AmountOff ->
        if (rule.amount <= 0) price else price - rule.amount

    // SPEC §2/§3: applies only when the price is strictly above `threshold` — a price exactly
    // equal to the threshold is left alone. A non-positive bonus is ignored, for the same
    // reason SPEC §5 gives for AmountOff.
    is DiscountRule.BigSpenderBonus ->
        if (price > rule.threshold && rule.amount > 0) price - rule.amount else price

    // Not described by SPEC.md — leave the price unchanged.
    is DiscountRule.Then -> price
    is DiscountRule.BestOf -> price
    is DiscountRule.OnlyIf -> price
}

/**
 * Integer division rounding towards negative infinity, so that "rounding down" (SPEC §2) still
 * rounds *down* on a negative price rather than towards zero as Kotlin's `/` would.
 */
private fun floorDiv(numerator: Int, denominator: Int): Int =
    Math.floorDiv(numerator, denominator)
