package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * Finds the cheapest offer for [duck] across the shops of [franchise].
 *
 * Shops that will not take the duck are skipped; promotions from the chain and from the shop both
 * apply. Returns `null` when the chain has nothing to offer.
 */
fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (duck.price == 0) return null

    var bestOffer: Offer? = null

    for (shop in franchise.shops) {
        if (!shop.admits(duck)) continue

        val totalPromotions = franchise.promotions + shop.promotions
        val price = priceFor(duck, totalPromotions)

        if (bestOffer == null || price <= bestOffer.price) {
            bestOffer = Offer(shop, price)
        }
    }

    return bestOffer
}

private fun applyRule(rule: DiscountRule, duck: Duck, price: Int): Int {
    return when (rule) {
        is DiscountRule.Percentage -> price - (price * rule.percent / 100)
        is DiscountRule.AmountOff -> price - rule.amount
        is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) price + rule.amount else price
        is DiscountRule.Then -> applyRule(rule.first, duck, applyRule(rule.second, duck, price))
        is DiscountRule.BestOf -> rule.options.minOfOrNull { applyRule(it, duck, price) } ?: price
        is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) applyRule(rule.rule, duck, price) else price
    }
}
