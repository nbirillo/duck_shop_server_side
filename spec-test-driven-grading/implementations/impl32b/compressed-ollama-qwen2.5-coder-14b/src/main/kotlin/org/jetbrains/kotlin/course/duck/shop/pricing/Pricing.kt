package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var price = duck.price

    for (rule in rules) {
        val discount = when (rule) {
            is DiscountRule.Percentage -> (price.toLong() * rule.percent / 100).toInt()
            is DiscountRule.AmountOff -> rule.amount
            is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount else 0
            else -> 0 // Leave the price unchanged for unknown rules
        }
        price = maxOf(0, price - discount)
    }

    return price
}
