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

        val basePrice = maxOf(0, duck.price)
        val finalPrice = applyRules(basePrice, shop.promotions + franchise.promotions, duck)

        if (bestOffer == null || finalPrice < bestOffer.price) {
            bestOffer = Offer(shop, finalPrice)
        }
    }

    return bestOffer
}

private fun applyRules(price: Int, rules: List<DiscountRule>, duck: Duck): Int {
    var currentPrice = price

    for (rule in rules) {
        currentPrice = maxOf(0, applyRule(rule, currentPrice, duck))
    }

    return currentPrice
}

private fun applyRule(rule: DiscountRule, price: Int, duck: Duck): Int {
    return when (rule) {
        is DiscountRule.Percentage -> applyPercentage(price, rule.percent)
        is DiscountRule.AmountOff -> applyAmountOff(price, rule.amount)
        is DiscountRule.BigSpenderBonus -> applyBigSpenderBonus(price, rule.threshold, rule.amount, duck.price)
        is DiscountRule.Then -> applyThen(rule.first, rule.second, price, duck)
        is DiscountRule.BestOf -> applyBestOf(rule.options, price, duck)
        is DiscountRule.OnlyIf -> applyOnlyIf(rule.condition, rule.rule, price, duck)
    }
}

private fun applyPercentage(price: Int, percent: Int): Int {
    return (price * (100 - percent)) / 100
}

private fun applyAmountOff(price: Int, amount: Int): Int {
    return maxOf(0, price - amount)
}

private fun applyBigSpenderBonus(price: Int, threshold: Int, amount: Int, duckPrice: Int): Int {
    return if (duckPrice >= threshold) price + amount else price
}

private fun applyThen(first: DiscountRule, second: DiscountRule, price: Int, duck: Duck): Int {
    val intermediatePrice = applyRule(first, price, duck)
    return applyRule(second, intermediatePrice, duck)
}

private fun applyBestOf(options: List<DiscountRule>, price: Int, duck: Duck): Int {
    var bestPrice = price

    for (option in options) {
        val optionPrice = applyRule(option, price, duck)
        if (optionPrice < bestPrice) {
            bestPrice = optionPrice
        }
    }

    return bestPrice
}

private fun applyOnlyIf(condition: AdmissionPolicy, rule: DiscountRule, price: Int, duck: Duck): Int {
    return if (condition.admits(duck)) {
        applyRule(rule, price, duck)
    } else {
        price
    }
}
