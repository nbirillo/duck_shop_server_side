package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.Duck

/**
 * Pricing, as settled in exercise 11.4. **Not part of the capstone** — treat it as existing code with a
 * contract, and use it rather than reimplementing it.
 *
 * Each decision below was open in the original brief and is now written down, which is the only reason
 * it can be relied on:
 *
 *  1. Rules **compound** — each sees the price left by the ones before it, not the shelf price.
 *  2. A percentage takes a **rounded-down discount**, which is not the same function as keeping a
 *     rounded-down remainder: at 95 with 10% off, `95 - floor(9.5) = 86`, while
 *     `floor(95 × 90 / 100) = 85`.
 *  3. Rounding therefore happens **per step**, since each step is a discount of its own.
 *  4. The big-spender threshold is tested against **the price the rule is applied to**, so an earlier
 *     discount can push a duck under it, and the comparison is **inclusive**.
 *  5. The result is **clamped at zero** and never exceeds the shelf price.
 *  6. `BestOf` gives the customer the **lowest resulting price**, decided at that point in the
 *     composition rather than globally.
 *  7. `OnlyIf` asks its condition about **the duck as given** — conditions see a duck, not a price.
 *
 * The multiplication is done in [Long] on purpose: `Int.MAX_VALUE * 10` overflows, and the answer for
 * a duck priced `Int.MAX_VALUE` at 10% off is 1932735283.
 */
fun priceFor(duck: Duck, rule: DiscountRule): Int = apply(duck, rule, duck.price)

/** The flat-list form the basic tier of 11.4 specifies: the rules run one after another. */
fun priceFor(duck: Duck, rules: List<DiscountRule>): Int =
    rules.fold(duck.price) { price, rule -> apply(duck, rule, price) }

private fun apply(duck: Duck, rule: DiscountRule, price: Int): Int = when (rule) {
    is DiscountRule.Percentage -> reduce(price, discount = (price.toLong() * rule.percent / 100).toInt())
    is DiscountRule.AmountOff -> reduce(price, discount = rule.amount)
    is DiscountRule.BigSpenderBonus ->
        if (price >= rule.threshold) reduce(price, discount = rule.amount) else price

    is DiscountRule.Then -> apply(duck, rule.second, apply(duck, rule.first, price))
    is DiscountRule.BestOf ->
        rule.options.minOfOrNull { apply(duck, it, price) } ?: price

    is DiscountRule.OnlyIf ->
        if (rule.condition.admits(duck)) apply(duck, rule.rule, price) else price
}

/** Clamped both ways: a discount never pushes the price below zero, and never raises it. */
private fun reduce(price: Int, discount: Int): Int = (price - discount).coerceIn(0, price)
