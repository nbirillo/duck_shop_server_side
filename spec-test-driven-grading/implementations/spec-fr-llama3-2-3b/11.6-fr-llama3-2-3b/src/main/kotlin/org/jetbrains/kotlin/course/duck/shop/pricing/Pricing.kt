package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.Accessory

fun applyDiscount(price: Int, rule: DiscountRule): Int {
    return when (rule) {
        is DiscountRule.Percentage -> price * (100 - rule.percent) / 100
        is DiscountRule.AmountOff -> price - rule.amount
        is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) price - rule.amount else price
        is DiscountRule.Then -> applyDiscount(applyDiscount(price, rule.first), rule.second)
        is DiscountRule.BestOf -> rule.options.minByOrNull { applyDiscount(price, it) }?.let { applyDiscount(price, it) } ?: price
        is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) applyDiscount(price, rule.rule) else price
    }
}

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    val applicableShops = franchise.shops.filter { it.admits(duck) }

    if (applicableShops.isEmpty()) return null

    var bestOffer: Offer? = null

    for (shop in applicableShops) {
        var price = duck.price
        for (rule in shop.promotions) {
            price = applyDiscount(price, rule)
        }
        val offer = Offer(shop, price)
        if (bestOffer == null || offer.price < bestOffer.price) {
            bestOffer = offer
        }
    }

    return bestOffer
}
