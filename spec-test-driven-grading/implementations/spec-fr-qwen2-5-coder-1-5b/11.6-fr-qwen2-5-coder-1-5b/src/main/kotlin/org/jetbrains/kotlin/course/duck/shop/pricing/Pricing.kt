package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    var bestOffer: Offer? = null

    for (shop in franchise.shops) {
        if (shop.admits(duck)) {
            val discountedPrice = calculateDiscountedPrice(duck.price, shop.promotions)
            if (discountedPrice <= duck.price) {
                return Offer(shop, discountedPrice)
            }
        }
    }

    return bestOffer
}

private fun calculateDiscountedPrice(originalPrice: Int, rules: List<DiscountRule>): Int {
    var price = originalPrice

    for (rule in rules) {
        price = when (rule) {
            is DiscountRule.Percentage -> price - (price * rule.percent / 100)
            is DiscountRule.AmountOff -> price - rule.amount
            is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) price - rule.amount else price
            is DiscountRule.Then -> calculateDiscountedPrice(price, listOf(rule.first, rule.second))
            is DiscountRule.BestOf -> rule.options.minByOrNull { calculateDiscountedPrice(price, listOf(it)) }?.let { calculateDiscountedPrice(price, listOf(it)) } ?: price
            is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) calculateDiscountedPrice(price, listOf(rule.rule)) else price
        }
    }

    return price
}
