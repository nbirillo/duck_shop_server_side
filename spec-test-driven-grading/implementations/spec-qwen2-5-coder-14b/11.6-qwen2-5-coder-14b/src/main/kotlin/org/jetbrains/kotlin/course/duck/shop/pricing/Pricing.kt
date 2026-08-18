package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.math.max

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val applicableShops = franchise.shops.filter { it.admits(duck) }
    if (applicableShops.isEmpty()) return null

    var bestOffer: Offer? = null

    for (shop in applicableShops) {
        val basePrice = duck.price
        val allRules = franchise.promotions + shop.promotions
        val finalPrice = applyDiscounts(basePrice, allRules, duck)

        if (bestOffer == null || finalPrice < bestOffer.price) {
            bestOffer = Offer(shop, finalPrice)
        }
    }

    return bestOffer
}

private fun applyDiscounts(price: Int, rules: List<DiscountRule>, duck: Duck): Int {
    var currentPrice = price

    for (rule in rules) {
        currentPrice = when (rule) {
            is DiscountRule.Percentage -> {
                val percent = max(0, rule.percent)
                currentPrice - (currentPrice * percent / 100)
            }
            is DiscountRule.AmountOff -> {
                val amount = max(0, rule.amount)
                max(0, currentPrice - amount)
            }
            is DiscountRule.BigSpenderBonus -> {
                if (currentPrice >= rule.threshold) {
                    currentPrice + rule.amount
                } else {
                    currentPrice
                }
            }
            is DiscountRule.Then -> {
                val firstApplied = applyDiscounts(currentPrice, listOf(rule.first), duck)
                applyDiscounts(firstApplied, listOf(rule.second), duck)
            }
            is DiscountRule.BestOf -> {
                if (rule.options.isEmpty()) currentPrice
                else rule.options.map { applyDiscounts(currentPrice, listOf(it), duck) }.minOrNull() ?: currentPrice
            }
            is DiscountRule.OnlyIf -> {
                if (rule.condition.admits(duck)) {
                    applyDiscounts(currentPrice, listOf(rule.rule), duck)
                } else {
                    currentPrice
                }
            }
            else -> currentPrice // For any unspecified cases, leave the price unchanged
        }
    }

    return currentPrice
}
