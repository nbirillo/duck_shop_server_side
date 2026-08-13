package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import kotlin.math.max
import kotlin.math.min

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var finalPrice = duck.price
    for (rule in rules) {
        finalPrice = max(0, finalPrice - discount(rule, finalPrice))
    }
    return finalPrice
}

private fun discount(rule: DiscountRule, price: Int): Int = when (rule) {
    is DiscountRule.Percentage -> (price * rule.percent / 100)
    is DiscountRule.AmountOff -> min(price, rule.amount)
    is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) rule.amount else 0
    else -> 0
}
