package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * Returns the final price of [duck] after applying every rule in [rules], in order, to the
 * running price, starting from the shelf price ([Duck.price]).
 *
 * Each rule discounts the price it is applied to; the price is clamped at 0 after every rule,
 * so the result is never negative. An empty [rules] list returns the shelf price unchanged.
 */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule ->
        (price - discountFor(rule, price)).coerceAtLeast(0L).toInt()
    }

/**
 * The amount [rule] takes off a price of [price].
 *
 * Computed in [Long] so that a percentage of a very large price cannot overflow before the
 * result is clamped back into [Int] range. Rule kinds the specification does not describe
 * discount nothing.
 */
private fun discountFor(rule: DiscountRule, price: Int): Long = when (rule) {
    is DiscountRule.Percentage -> price.toLong() * rule.percent / 100
    is DiscountRule.AmountOff -> rule.amount.toLong()
    is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount.toLong() else 0L
    is DiscountRule.Then, is DiscountRule.BestOf, is DiscountRule.OnlyIf -> 0L
}
