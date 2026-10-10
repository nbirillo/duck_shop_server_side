package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var currentPrice = duck.price

    for (rule in rules) {
        when (rule) {
            is DiscountRule.Percentage -> {
                if (rule.percent > 0) {
                    currentPrice -= (currentPrice * rule.percent / 100)
                }
            }
            is DiscountRule.AmountOff -> {
                if (rule.amount > 0) {
                    currentPrice -= rule.amount
                }
            }
            is DiscountRule.BigSpenderBonus -> {
                if (currentPrice > rule.threshold) {
                    currentPrice -= rule.amount
                }
            }
            else -> {
                // Ignore unknown discount rules
            }
        }
    }

    return currentPrice
}
