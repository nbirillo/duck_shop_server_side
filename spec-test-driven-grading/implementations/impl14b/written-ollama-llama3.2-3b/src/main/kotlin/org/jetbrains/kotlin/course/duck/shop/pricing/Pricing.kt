package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var finalPrice = duck.price

    for (rule in rules) {
        when (rule) {
            is DiscountRule.Percentage -> {
                if (rule.percent > 0) {
                    finalPrice -= (finalPrice * rule.percent) / 100
                }
            }
            is DiscountRule.AmountOff -> {
                if (rule.amount > 0) {
                    finalPrice -= rule.amount
                }
            }
            is DiscountRule.BigSpenderBonus -> {
                if (finalPrice >= rule.threshold) {
                    finalPrice -= rule.amount
                }
            }
            else -> {}
        }
    }

    return finalPrice
}
