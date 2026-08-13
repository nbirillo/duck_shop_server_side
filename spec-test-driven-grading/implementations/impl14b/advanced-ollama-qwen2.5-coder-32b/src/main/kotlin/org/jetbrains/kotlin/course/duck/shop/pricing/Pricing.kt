package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck
import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy

fun priceFor(duck: Duck, rule: DiscountRule): Int {
    return when (rule) {
        is DiscountRule.Percentage -> {
            val discountAmount = (duck.price * rule.percent) / 100
            maxOf(0, duck.price - discountAmount)
        }
        is DiscountRule.AmountOff -> maxOf(0, duck.price - rule.amount)
        is DiscountRule.BigSpenderBonus -> {
            if (duck.price >= rule.threshold) {
                maxOf(0, duck.price - rule.amount)
            } else {
                duck.price
            }
        }
        is DiscountRule.Then -> priceFor(duck, priceFor(duck, rule.first))
        is DiscountRule.BestOf -> rule.options.minOfOrNull { priceFor(duck, it) } ?: duck.price
        is DiscountRule.OnlyIf -> if (rule.condition.admits(duck)) {
            priceFor(duck, rule.rule)
        } else {
            duck.price
        }
    }
}

fun priceFor(duck: Duck, rules: List<DiscountRule>): Int {
    return rules.fold(duck.price) { currentPrice, rule ->
        priceFor(Duck(duck.name, currentPrice, duck.hasKotlinAttribute, duck.accessories), rule)
    }
}
