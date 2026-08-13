package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import kotlin.math.max

fun priceFor(duck: Duck, rule: DiscountRule): Int {
    return when (rule) {
        is DiscountRule.Percentage -> {
            val discountAmount = (duck.price * rule.percent) / 100
            duck.price - discountAmount
        }
        is DiscountRule.AmountOff -> max(0, duck.price - rule.amount)
        is DiscountRule.BigSpenderBonus -> {
            if (duck.price >= rule.threshold) {
                max(0, duck.price - rule.amount)
            } else {
                duck.price
            }
        }
        is DiscountRule.Then -> {
            val priceAfterFirst = priceFor(duck, rule.first)
            priceFor(Duck(duck.name, priceAfterFirst, duck.hasKotlinAttribute, duck.accessories), rule.second)
        }
        is DiscountRule.BestOf -> {
            if (rule.options.isEmpty()) {
                duck.price
            } else {
                rule.options.minOf { priceFor(duck, it) }
            }
        }
        is DiscountRule.OnlyIf -> {
            if (rule.condition.admits(duck)) {
                priceFor(duck, rule.rule)
            } else {
                duck.price
            }
        }
    }
}

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    return rules.fold(duck.price) { currentPrice, rule ->
        priceFor(Duck(duck.name, currentPrice, duck.hasKotlinAttribute, duck.accessories), rule)
    }
}
