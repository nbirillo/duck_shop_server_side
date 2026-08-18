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
        val promotions = franchise.promotions + shop.promotions
        val finalPrice = applyDiscounts(basePrice, promotions, duck)

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
            is DiscountRule.Percentage -> max(0, currentPrice - (currentPrice * rule.percent / 100))
            is DiscountRule.AmountOff -> max(0, currentPrice - rule.amount)
            is DiscountRule.BigSpenderBonus -> if (currentPrice > rule.threshold) max(0, currentPrice - rule.amount) else currentPrice
            is DiscountRule.Then -> applyDiscounts(applyDiscounts(currentPrice, listOf(rule.first), duck), listOf(rule.second), duck)
            is DiscountRule.BestOf -> rule.options.minByOrNull { applyDiscounts(currentPrice, listOf(it), duck) }?.let { applyDiscounts(currentPrice, listOf(it), duck) } ?: currentPrice
            is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) applyDiscounts(currentPrice, listOf(rule.rule), duck) else currentPrice
        }
    }

    return currentPrice
}
