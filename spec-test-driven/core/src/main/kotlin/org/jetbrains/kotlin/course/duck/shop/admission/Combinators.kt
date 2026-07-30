package org.jetbrains.kotlin.course.duck.shop.admission

// Combinator policies — compose other policies into richer specifications.

/**
 * Admits a duck only when **every** policy in [policies] admits it (logical AND).
 */
class All(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())

    override fun admits(duck: Duck): Boolean = TODO("implement All")
}

/**
 * Admits a duck when **at least one** policy in [policies] admits it (logical OR).
 */
class Any(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())

    override fun admits(duck: Duck): Boolean = TODO("implement Any")
}

/**
 * Admits exactly the ducks that [policy] does **not** admit (logical NOT).
 */
class Not(private val policy: AdmissionPolicy) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = TODO("implement Not")
}
