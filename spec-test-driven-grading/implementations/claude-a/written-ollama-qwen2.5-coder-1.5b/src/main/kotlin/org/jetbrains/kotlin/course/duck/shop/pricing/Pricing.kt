package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * Returns the price of [duck] after applying [rules].
 *
 * The rules are applied **sequentially, in list order**: each rule discounts the price the
 * previous one produced. An empty list leaves the price unchanged.
 *
 * Decisions the specification leaves open:
 * - **Rounding.** [DiscountRule.Percentage] truncates, so 10% off 95 is 85.
 * - **Floor.** The price never drops below 0; a discount larger than the price yields 0.
 * - **Overflow.** Arithmetic runs in [Long] and the result is clamped back into [Int].
 * - **Unspecified rules.** [DiscountRule.Then], [DiscountRule.BestOf] and
 *   [DiscountRule.OnlyIf] leave the price unchanged.
 */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule -> price.applying(rule) }

/** Returns this price with a single [rule] applied. */
private fun Int.applying(rule: DiscountRule): Int = when (rule) {
    is DiscountRule.Percentage -> reducedBy(this.toLong() * rule.percent / 100)

    is DiscountRule.AmountOff -> reducedBy(rule.amount.toLong())

    // A duck only earns the bonus once it reaches the threshold; below it, nothing happens.
    is DiscountRule.BigSpenderBonus ->
        if (this >= rule.threshold) reducedBy(rule.amount.toLong()) else this

    // Not described by the specification — leave the price alone.
    is DiscountRule.Then, is DiscountRule.BestOf, is DiscountRule.OnlyIf -> this
}

/** Returns this price less [off], clamped to a non-negative [Int]. */
private fun Int.reducedBy(off: Long): Int =
    (this - off).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
