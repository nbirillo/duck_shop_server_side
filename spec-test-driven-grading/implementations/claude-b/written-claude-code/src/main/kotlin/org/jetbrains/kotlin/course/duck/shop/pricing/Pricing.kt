package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

// Implement these from SPEC.md. Where the specification does not settle something, decide
// and move on — a gap is not a reason to stop, and what you decide is exactly what the
// exercise is looking at.
//
// DiscountRule may have cases SPEC.md never describes. Leave the price unchanged for those:
// your `when` still has to be exhaustive.

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule -> (price - discountOf(rule, price)).coerceAtLeast(0) }

/**
 * The discount [rule] takes off [price] — the price the rule is applied to (B1).
 *
 * The returned discount is never larger than [price], so the subtraction in [priceFor] cannot
 * underflow; clamping at 0 still happens there (B9).
 */
private fun discountOf(rule: DiscountRule, price: Int): Int = when (rule) {
    // B2: the *discount* rounds down, computed on a Long so `price * percent` cannot overflow (E14, E15).
    is DiscountRule.Percentage -> (price.toLong() * rule.percent / 100).coerceAtMost(price.toLong()).toInt()
    // B3
    is DiscountRule.AmountOff -> rule.amount
    // B4, B5: inclusive threshold, tested against the running price (B6).
    is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount else 0
    // Cases SPEC.md does not describe: no discount, price unchanged.
    is DiscountRule.Then, is DiscountRule.BestOf, is DiscountRule.OnlyIf -> 0
}
