package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var price = duck.price

    for (rule in rules) {
        when (rule) {
            is DiscountRule.Percentage -> {
                if (rule.percent > 0) {
                    price -= (price * rule.percent / 100)
                }
            }
            is DiscountRule.AmountOff -> {
                price -= rule.amount
            }
            is DiscountRule.BigSpenderBonus -> {
                if (price >= rule.threshold) {
                    price -= rule.amount
                }
            }
            else -> {} // Leave the price unchanged for unknown rules
        }
    }

    return price
}
