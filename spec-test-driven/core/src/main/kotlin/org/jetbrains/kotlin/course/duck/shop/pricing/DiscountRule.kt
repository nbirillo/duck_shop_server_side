package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy

/**
 * A promotion a shop can have active.
 *
 * These are **data only** — deliberately. What a rule *does* to a price is not defined here, because
 * defining it is exercise 11.4: the learner writes that contract from an informal brief, and only then
 * does anyone implement it.
 *
 * The three leaves are the promotions the business described. The three combinators are the advanced
 * tier, where promotions stop being a flat list and start composing — the same skeleton as
 * [AdmissionPolicy]'s `AllOf`/`AnyOf`/`Not`, except these combine values rather than yes/no answers.
 */
sealed interface DiscountRule {

    /** Takes some percentage off. */
    data class Percentage(val percent: Int) : DiscountRule

    /** Takes a flat amount off. */
    data class AmountOff(val amount: Int) : DiscountRule

    /** Takes [amount] off a duck that is expensive enough. */
    data class BigSpenderBonus(val threshold: Int, val amount: Int) : DiscountRule

    /** Runs [first], then [second]. */
    data class Then(val first: DiscountRule, val second: DiscountRule) : DiscountRule

    /** Offers all of [options] and lets the customer have whichever turns out best. */
    data class BestOf(val options: List<DiscountRule>) : DiscountRule

    /** Runs [rule], but only for ducks that [condition] admits. */
    data class OnlyIf(val condition: AdmissionPolicy, val rule: DiscountRule) : DiscountRule
}
