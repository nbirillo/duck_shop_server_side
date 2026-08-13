package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import kotlin.math.max
import kotlin.math.min

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    var currentPrice = duck.price
    for (rule in rules) {
        currentPrice = applyRule(currentPrice, rule)
    }
    return max(0, currentPrice)
}

private fun applyRule(price: Int, rule: DiscountRule): Int {
    return when (rule) {
        is DiscountRule.Percentage -> price - (price * rule.percent / 100)
        is DiscountRule.AmountOff -> price - rule.amount
        is DiscountRule.BigSpenderBonus -> if (price >= rule.threshold) price - rule.amount else price
        else -> price // For any other cases not described in the specification
    }
}
