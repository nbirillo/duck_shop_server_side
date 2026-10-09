package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy
import org.jetbrains.kotlin.course.duck.shop.admission.Accessory

fun bestOffer(duck: Duck, franchise: Franchise): Offer? {
    if (!franchise.admissionPolicy.admits(duck)) return null

    var bestOffer: Offer? = null

    for (shop in franchise.shops) {
        if (!shop.admits(duck)) continue

        val totalPromotions = shop.promotions + franchise.promotions
        val price = priceFor(duck, totalPromotions)

        if (bestOffer == null || price < bestOffer.price) {
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
