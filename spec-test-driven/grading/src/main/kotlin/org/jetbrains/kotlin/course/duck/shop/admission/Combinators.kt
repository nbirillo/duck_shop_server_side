package org.jetbrains.kotlin.course.duck.shop.admission

// Reference implementation of the combinator policies (teacher-only, :grading). The idiomatic
// stdlib all{}/any{} give the vacuous-truth results (AllOf(emptyList())==true,
// AnyOf(emptyList())==false) for free.

/**
 * Admits a duck only when **every** policy in [policies] admits it (logical AND).
 */
class AllOf(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())

    override fun admits(duck: Duck): Boolean = policies.all { it.admits(duck) }
}

/**
 * Admits a duck when **at least one** policy in [policies] admits it (logical OR).
 */
class AnyOf(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())

    override fun admits(duck: Duck): Boolean = policies.any { it.admits(duck) }
}

/**
 * Admits exactly the ducks that [policy] does **not** admit (logical NOT).
 */
class Not(private val policy: AdmissionPolicy) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = !policy.admits(duck)
}
