package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var currentPrice = duck.price
    for (rule in rules) {
        currentPrice = when (rule) {
            is DiscountRule.Percentage -> maxOf(0, currentPrice - (currentPrice.toLong() * rule.percent / 100).toInt())
            is DiscountRule.AmountOff -> maxOf(0, currentPrice - rule.amount)
            is DiscountRule.BigSpenderBonus -> if (currentPrice >= rule.threshold) maxOf(0, currentPrice - rule.amount) else currentPrice
            else -> currentPrice // For any other cases not described in the specification
        }
    }
    return currentPrice
}
