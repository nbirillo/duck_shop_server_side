package org.jetbrains.kotlin.course.duck.shop.admission

/**
 * A rule for deciding whether a [Duck] may be admitted to a shop.
 *
 * This is the **Specification pattern**: each policy answers one yes/no question about
 * a duck, and policies can be combined into richer rules using the [All], [Any] and [Not]
 * combinators.
 */
fun interface AdmissionPolicy {
    /**
     * Returns `true` when [duck] satisfies this policy and may be admitted, or `false` otherwise.
     */
    fun admits(duck: Duck): Boolean
}
