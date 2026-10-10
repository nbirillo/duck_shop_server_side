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
                if (finalPrice < 0) finalPrice = 0
            }
            is DiscountRule.BigSpenderBonus -> {
                // This rule will be applied last after all other rules have been processed.
            }
            else -> {
                // Leave the price unchanged for any other types of discount rules.
            }
        }
    }

    // Apply big-spender bonus last if applicable
    for (rule in rules) {
        if (rule is DiscountRule.BigSpenderBonus && finalPrice >= rule.threshold) {
            finalPrice -= rule.amount
            if (finalPrice < 0) finalPrice = 0
        }
    }

    return finalPrice
}
