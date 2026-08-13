package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var price = duck.price
    for (rule in rules) {
        val discount = when (rule) {
            is DiscountRule.Percentage -> (price * rule.percent / 100).coerceAtLeast(0)
            is DiscountRule.AmountOff -> rule.amount.coerceAtMost(price)
            is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount else 0
            else -> 0
        }
        price = (price - discount).coerceAtLeast(0)
    }
    return price
}
