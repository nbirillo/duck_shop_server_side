package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import kotlin.math.floor

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var currentPrice = duck.price
    for (rule in rules) {
        currentPrice = maxOf(0, currentPrice - discount(rule, currentPrice))
    }
    return currentPrice
}

private fun discount(rule: DiscountRule, price: Int): Int = when (rule) {
    is DiscountRule.Percentage -> floor((price * rule.percent).toDouble() / 100).toInt()
    is DiscountRule.AmountOff -> minOf(price, rule.amount)
    is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount else 0
    else -> 0
}
