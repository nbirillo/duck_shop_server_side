package org.jetbrains.kotlin.course.duck.shop.admission

/**
 * A rule that decides whether a [Duck] may be admitted to a shop.
 *
 * This is the **Specification pattern**: each policy answers a single yes/no question about
 * a duck, and policies compose into richer rules through the [All], [Any] and [Not]
 * combinators.
 */
fun interface AdmissionPolicy {
    /**
     * Returns `true` if [duck] satisfies this policy and may be admitted, `false` otherwise.
     */
    fun admits(duck: Duck): Boolean
}
