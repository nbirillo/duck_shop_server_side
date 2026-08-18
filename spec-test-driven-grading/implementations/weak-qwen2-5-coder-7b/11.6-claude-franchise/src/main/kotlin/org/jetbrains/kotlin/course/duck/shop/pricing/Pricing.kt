package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import org.jetbrains.kotlin.course.duck.shop.admission.Franchise
import org.jetbrains.kotlin.course.duck.shop.admission.Offer
import org.jetbrains.kotlin.course.duck.shop.admission.DiscountRule

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val eligibleShops = franchise.shops.filter { franchise.admissionPolicy.admits(duck) && it.admits(duck) }
    
    if (eligibleShops.isEmpty()) return null
    
    var bestShop: Shop? = null
    var bestPrice = Int.MAX_VALUE
    
    for (shop in eligibleShops) {
        val price = applyDiscounts(duck, shop.promotions + franchise.promotions, duck.price)
        if (price < bestPrice || (price == bestPrice && franchise.shops.indexOf(shop) < franchise.shops.indexOf(bestShop))) {
            bestShop = shop
            bestPrice = price
        }
    }
    
    return bestShop?.let { Offer(it, bestPrice) }
}

private fun applyDiscounts(duck: Duck, rules: List<DiscountRule>, price: Int): Int {
    var currentPrice = maxOf(price, 0)
    for (rule in rules) {
        currentPrice = when (rule) {
            is DiscountRule.Percentage -> maxOf(currentPrice * (100 - rule.percent) / 100, 0)
            is DiscountRule.AmountOff -> maxOf(currentPrice - rule.amount, 0)
            is DiscountRule.BigSpenderBonus -> if (currentPrice > rule.threshold) currentPrice - rule.amount else currentPrice
            is DiscountRule.Then -> applyDiscounts(duck, listOf(rule.first), applyDiscounts(duck, listOf(rule.second), price))
            is DiscountRule.BestOf -> rules.minByOrNull { applyDiscounts(duck, listOf(it), price) }?.let { applyDiscounts(duck, listOf(it), price) } ?: currentPrice
            is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) applyDiscounts(duck, listOf(rule.rule), price) else currentPrice
        }
    }
    return minOf(currentPrice, Int.MAX_VALUE)
}
