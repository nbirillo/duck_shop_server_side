package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

// Implement these from SPEC.md. Where the specification does not settle something, decide
// and move on — a gap is not a reason to stop, and what you decide is exactly what the
// exercise is looking at.
//
// DiscountRule may have cases SPEC.md never describes. Leave the price unchanged for those:
// your `when` still has to be exhaustive.

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule ->
        (price - discountOf(rule, price)).coerceAtLeast(0)
    }

/** The discount [rule] takes off [price] — the price the rule is applied to (B1). */
private fun discountOf(rule: DiscountRule, price: Int): Int = when (rule) {
    // floor(price * percent / 100), computed in Long so large prices do not overflow (B2, E7).
    is DiscountRule.Percentage -> (price.toLong() * rule.percent / 100).coerceAtMost(price.toLong()).toInt()
    is DiscountRule.AmountOff -> rule.amount
    // Inclusive test against the running price, at this rule's own position (B4, B5, E8).
    is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount else 0
    // Cases SPEC.md does not describe: no discount.
    is DiscountRule.Then, is DiscountRule.BestOf, is DiscountRule.OnlyIf -> 0
}
