package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.math.max
import kotlin.math.min

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val eligibleShops = franchise.shops.filter { franchise.admissionPolicy.admits(duck) && it.admits(duck) }
    
    if (eligibleShops.isEmpty()) return null
    
    var bestShop: Shop? = null
    var bestPrice = Int.MAX_VALUE
    
    for (shop in eligibleShops) {
        val shopPromotions = shop.promotions
        val chainPromotions = franchise.promotions
        
        var price = max(0, duck.price)
        
        for (rule in shopPromotions + chainPromotions) {
            price = applyRule(rule, duck, price)
        }
        
        if (price < bestPrice) {
            bestPrice = price
            bestShop = shop
        }
    }
    
    return bestShop?.let { Offer(it, bestPrice) }
}

private fun applyRule(rule: DiscountRule, duck: Duck, incomingPrice: Int): Int {
    val outgoingPrice = when (rule) {
        is DiscountRule.Percentage -> max(0, (incomingPrice * (100 - rule.percent)) / 100)
        is DiscountRule.AmountOff -> max(0, incomingPrice - rule.amount)
        is DiscountRule.BigSpenderBonus -> if (incomingPrice >= rule.threshold) max(0, incomingPrice - rule.amount) else incomingPrice
        is DiscountRule.Then -> {
            val firstApplied = applyRule(rule.first, duck, incomingPrice)
            applyRule(rule.second, duck, firstApplied)
        }
        is DiscountRule.BestOf -> if (rule.options.isEmpty()) incomingPrice else rule.options.map { applyRule(it, duck, incomingPrice) }.minOrNull() ?: incomingPrice
        is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) applyRule(rule.rule, duck, incomingPrice) else incomingPrice
        else -> incomingPrice // For any unknown cases
    }
    
    return min(outgoingPrice, Int.MAX_VALUE)
}
