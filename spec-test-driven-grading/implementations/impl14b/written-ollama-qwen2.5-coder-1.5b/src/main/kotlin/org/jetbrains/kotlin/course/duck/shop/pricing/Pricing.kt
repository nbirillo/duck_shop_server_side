package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.DiscountRule

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var finalPrice = duck.price

    for (rule in rules) {
        when (rule) {
            is DiscountRule.Percentage -> {
                finalPrice -= (finalPrice * rule.percent / 100).coerceAtLeast(0)
            }
            is DiscountRule.AmountOff -> {
                finalPrice -= rule.amount.coerceAtMost(finalPrice)
            }
            is DiscountRule.BigSpenderBonus -> {
                if (finalPrice >= rule.threshold) {
                    finalPrice -= rule.amount
                }
            }
            else -> {}
        }
    }

    return finalPrice.coerceAtLeast(0)
}
