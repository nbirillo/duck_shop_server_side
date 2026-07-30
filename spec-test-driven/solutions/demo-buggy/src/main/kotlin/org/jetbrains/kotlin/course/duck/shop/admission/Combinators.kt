package org.jetbrains.kotlin.course.duck.shop.admission

// demo-buggy combinators — these are correct; the only seeded bug is in Leaves.kt (MaxBudget).

class AllOf(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())

    override fun admits(duck: Duck): Boolean = policies.all { it.admits(duck) }
}

class AnyOf(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())

    override fun admits(duck: Duck): Boolean = policies.any { it.admits(duck) }
}

class Not(private val policy: AdmissionPolicy) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = !policy.admits(duck)
}
