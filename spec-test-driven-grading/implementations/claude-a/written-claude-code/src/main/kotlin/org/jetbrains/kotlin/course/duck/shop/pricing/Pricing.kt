package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * Returns what [duck] actually costs once every rule in [rules] has been applied.
 *
 * The shelf price ([Duck.price]) is the starting point, and the rules are applied in list
 * order, each one discounting the price the rules before it left behind. Order therefore
 * matters, duplicates compound, and the result is clamped to `0 .. duck.price`.
 *
 * Only [Duck.price] is read; the other duck fields never affect the answer.
 */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule -> price.discountedBy(rule) }

/**
 * Returns the price left after [rule] discounts this price, never below 0.
 *
 * The discount is computed in [Long] arithmetic: a percentage of a large price overflows the
 * 32-bit range long before the (always in-range) result does.
 */
private fun Int.discountedBy(rule: DiscountRule): Int {
    val discount: Long = when (rule) {
        // The discount, not the surviving fraction, is rounded down: 10% of 95 is 9, so 95 -> 86.
        is DiscountRule.Percentage -> this.toLong() * rule.percent / 100
        is DiscountRule.AmountOff -> rule.amount.toLong()
        // Inclusive threshold, tested against the price the rule is applied to.
        is DiscountRule.BigSpenderBonus -> if (this >= rule.threshold) rule.amount.toLong() else 0L
        // SPEC.md describes no discount for these combinators, so they leave the price alone.
        is DiscountRule.Then, is DiscountRule.BestOf, is DiscountRule.OnlyIf -> 0L
    }
    return maxOf(0L, this - discount).toInt()
}
