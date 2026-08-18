package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var price = duck.price
    for (rule in rules) {
        price = applyDiscount(price, duck, rule)
    }
    return price
}

private fun applyDiscount(price: Int, duck: Duck, rule: DiscountRule): Int {
    return when (rule) {
        is DiscountRule.Percentage -> price - (price * rule.percent / 100)
        is DiscountRule.AmountOff -> price - rule.amount
        is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) price - rule.amount else price
        is DiscountRule.Then -> applyDiscount(applyDiscount(price, duck, rule.first), duck, rule.second)
        is DiscountRule.BestOf -> rule.options.map { applyDiscount(price, duck, it) }.minOrNull() ?: price
        is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) applyDiscount(price, duck, rule.rule) else price
        else -> price // For any other cases not described in the specification
    }
}

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val allShops = franchise.shops + listOf(franchise)
    var bestOffer: Offer? = null

    for (shop in allShops) {
        if (!shop.admits(duck)) continue

        val discountedPrice = priceFor(duck, shop.promotions)
        if (discountedPrice <= 0) continue

        val offer = Offer(shop, discountedPrice)
        if (bestOffer == null || offer.price < bestOffer.price) {
            bestOffer = offer
        }
    }

    return bestOffer
}
