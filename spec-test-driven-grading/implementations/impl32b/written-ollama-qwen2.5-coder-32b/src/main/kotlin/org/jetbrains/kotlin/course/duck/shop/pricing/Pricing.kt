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
                finalPrice = maxOf(0, finalPrice - rule.amount)
            }
            is DiscountRule.BigSpenderBonus -> {
                // Big-spender bonus is applied last
                continue
            }
            else -> {
                // Leave the price unchanged for unknown rules
            }
        }
    }

    // Apply big-spender bonus if applicable
    for (rule in rules) {
        if (rule is DiscountRule.BigSpenderBonus && duck.price >= rule.threshold) {
            finalPrice = maxOf(0, finalPrice - rule.amount)
        }
    }

    return finalPrice
}
