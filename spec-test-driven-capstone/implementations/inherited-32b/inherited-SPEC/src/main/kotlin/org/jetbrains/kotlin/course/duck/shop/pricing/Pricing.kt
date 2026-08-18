package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.math.max
import kotlin.math.min

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var price = max(0, duck.price)
    for (rule in rules) {
        price = when (rule) {
            is DiscountRule.Percentage -> max(0, price - (price * rule.percent / 100))
            is DiscountRule.AmountOff -> max(0, price - rule.amount)
            is DiscountRule.BigSpenderBonus -> if (price > rule.threshold) max(0, price - rule.amount) else price
            is DiscountRule.Then -> {
                val firstPrice = priceFor(duck, listOf(rule.first))
                priceFor(duck.copy(price = firstPrice), listOf(rule.second))
            }
            is DiscountRule.BestOf -> if (rule.options.isEmpty()) price else rule.options.map { priceFor(duck, listOf(it)) }.minOrNull() ?: price
            is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) priceFor(duck, listOf(rule.rule)) else price
            else -> price // Leave unchanged for unknown cases
        }
    }
    return min(price, Int.MAX_VALUE)
}

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val eligibleShops = franchise.shops.filter { franchise.admissionPolicy.admits(duck) && it.admits(duck) }

    if (eligibleShops.isEmpty()) {
        return null
    }

    var bestShop: Shop? = null
    var bestPrice = Int.MAX_VALUE

    for (shop in eligibleShops) {
        val shopPromotions = shop.promotions + franchise.promotions
        val price = priceFor(duck, shopPromotions)

        if (price < bestPrice || (price == bestPrice && bestShop == null)) {
            bestPrice = price
            bestShop = shop
        }
    }

    return if (bestShop != null) Offer(bestShop, bestPrice) else null
}
