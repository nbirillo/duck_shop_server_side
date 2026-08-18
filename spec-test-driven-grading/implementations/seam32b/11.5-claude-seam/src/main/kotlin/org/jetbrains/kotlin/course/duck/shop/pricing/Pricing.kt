package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Accessory
import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.Shop
import kotlin.math.floor

fun quote(duck: Duck, shop: Shop, promotions: List<DiscountRule>): Int? {
    if (!shop.admits(duck)) return null

    var price = maxOf(0, duck.price)

    for (rule in promotions) {
        price = apply(rule, duck, price)
    }

    return price
}

private fun apply(rule: DiscountRule, duck: Duck, price: Int): Int {
    return when (rule) {
        is DiscountRule.Percentage -> {
            val reduction = floor(price * rule.percent.toDouble() / 100).toInt()
            price - clamp(reduction, 0, price)
        }
        is DiscountRule.AmountOff -> {
            price - clamp(rule.amount, 0, price)
        }
        is DiscountRule.BigSpenderBonus -> {
            if (price >= rule.threshold) {
                price - clamp(rule.amount, 0, price)
            } else {
                price
            }
        }
        is DiscountRule.Then -> {
            val firstApplied = apply(rule.first, duck, price)
            apply(rule.second, duck, firstApplied)
        }
        is DiscountRule.BestOf -> {
            rule.options.minOfOrNull { apply(it, duck, price) } ?: price
        }
        is DiscountRule.OnlyIf -> {
            if (rule.condition.admits(duck)) {
                apply(rule.rule, duck, price)
            } else {
                price
            }
        }
        else -> price // Leave unchanged for any unknown cases
    }
}

private fun clamp(value: Int, min: Int, max: Int): Int {
    return value.coerceIn(min, max)
}
