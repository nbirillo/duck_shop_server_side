package org.jetbrains.kotlin.course.duck.shop.pricing

import org.jetbrains.kotlin.course.duck.shop.admission.AdmissionPolicy

/**
 * A promotion that a shop can have active.
 *
 * These are **data only**, by design. What a rule *does* to a price is not defined here, because
 * defining that behaviour is the spec-writing exercise: the learner writes the contract from an
 * informal brief, and only then does anyone implement it.
 *
 * The three leaves are the promotions described by the business. The three combinators belong to
 * the advanced tier, where promotions move beyond a flat list and start composing — following the
 * same structure as [AdmissionPolicy]'s `AllOf`/`AnyOf`/`Not`, but combining values rather than
 * yes/no answers.
 */
sealed interface DiscountRule {

    /** Takes a percentage off. */
    data class Percentage(val percent: Int) : DiscountRule

    /** Takes a fixed amount off. */
    data class AmountOff(val amount: Int) : DiscountRule

    /** Takes [amount] off a duck that is expensive enough. */
    data class BigSpenderBonus(val threshold: Int, val amount: Int) : DiscountRule

    /** Runs [first], followed by [second]. */
    data class Then(val first: DiscountRule, val second: DiscountRule) : DiscountRule

    /** Offers all of [options] and lets the customer choose whichever turns out best. */
    data class BestOf(val options: List<DiscountRule>) : DiscountRule

    /** Runs [rule], but only for ducks admitted by [condition]. */
    data class OnlyIf(val condition: AdmissionPolicy, val rule: DiscountRule) : DiscountRule
}
