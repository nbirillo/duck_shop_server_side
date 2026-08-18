package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.Accessory

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val eligibleShops = franchise.shops.filter { s -> s.admits(duck) }
    
    if (eligibleShops.isEmpty()) return null
    
    var cheapestShop = eligibleShops[0]
    var cheapestPrice = maxOf(0, duck.price)
    
    for (s in eligibleShops) {
        val price = applyDiscountRules(s.promotions + franchise.promotions, duck, cheapestPrice)
        if (price < cheapestPrice) {
            cheapestShop = s
            cheapestPrice = price
        }
    }
    
    return Offer(cheapestShop, cheapestPrice)
}

fun applyDiscountRules(rules: List<DiscountRule>, duck: Duck, basePrice: Int): Int {
    var price = basePrice
    
    for (rule in rules) {
        when (rule) {
            is DiscountRule.Percentage -> price *= rule.percent / 100
            is DiscountRule.AmountOff -> price -= rule.amount
            is DiscountRule.BigSpenderBonus -> if (duck.price >= rule.threshold) price += rule.amount
            is DiscountRule.Then -> price = maxOf(price, applyDiscountRules(rule.first.promotions + rule.second.promotions, duck, price))
            is DiscountRule.BestOf -> {
                val bestPrice = applyDiscountRules(rule.options, duck, basePrice)
                if (bestPrice < price) price = bestPrice
            }
            is DiscountRule.OnlyIf -> if (!rule.condition.admits(duck)) continue
            else price = applyDiscountRules(rule.rule.promotions + rule.second.promotions, duck, price)
        }
    }
    
    return maxOf(price, 0)
}
