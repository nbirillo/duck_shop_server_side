package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var finalPrice = duck.price

    for (rule in rules) {
        when (rule) {
            is DiscountRule.Percentage -> {
                val discountAmount = (finalPrice * rule.percent) / 100
                finalPrice -= discountAmount
            }
            is DiscountRule.AmountOff -> {
                finalPrice -= rule.amount
            }
            is DiscountRule.BigSpenderBonus -> {
                if (finalPrice >= rule.threshold) {
                    finalPrice -= rule.amount
                }
            }
            else -> {
                // Leave the price unchanged for unknown rules
            }
        }
    }

    return finalPrice
}
