package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * Returns the final price of [duck] after applying every applicable rule in [rules] to its
 * shelf price.
 *
 * Rules are applied in list order, each one reducing the price left by the previous ones,
 * except for [DiscountRule.BigSpenderBonus] rules, which are deferred and applied last
 * (still in list order among themselves). The result is never negative.
 *
 * Rule shapes the specification does not describe ([DiscountRule.Then],
 * [DiscountRule.BestOf], [DiscountRule.OnlyIf]) leave the price unchanged.
 */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    // A big-spender bonus goes last, after everything else has been processed.
    val (bonuses, ordinary) = rules.partition { it is DiscountRule.BigSpenderBonus }
    var price = duck.price
    for (rule in ordinary + bonuses) {
        price = rule.applyTo(price, shelfPrice = duck.price)
    }
    return price
}

/**
 * Applies this single rule to [price], the price left by the rules before it.
 *
 * [shelfPrice] is the duck's original, undiscounted price; only the big-spender threshold is
 * measured against it.
 */
private fun DiscountRule.applyTo(price: Int, shelfPrice: Int): Int = when (this) {
    // `percent` percent of the running price, the reduction itself rounded down.
    is DiscountRule.Percentage -> (price - price * percent / 100).coerceAtLeast(0)

    is DiscountRule.AmountOff -> (price - amount).coerceAtLeast(0)

    is DiscountRule.BigSpenderBonus ->
        if (shelfPrice >= threshold) (price - amount).coerceAtLeast(0) else price

    // Not described by the specification: leave the price alone.
    is DiscountRule.Then, is DiscountRule.BestOf, is DiscountRule.OnlyIf -> price
}
