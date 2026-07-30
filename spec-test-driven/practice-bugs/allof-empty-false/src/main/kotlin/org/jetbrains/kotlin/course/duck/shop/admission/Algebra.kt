package org.jetbrains.kotlin.course.duck.shop.admission

// A deliberately BROKEN copy of the admission-policy algebra used as a practice target: the
// learner's tests should CATCH the bug (some test must fail here). The ONLY bug is in AllOf,
// which returns false for an EMPTY list instead of the vacuous-truth `true`. A good suite has a
// test for AllOf over an empty list that catches this.

data class Accessory(val name: String)

data class Duck(
    val name: String,
    val price: Int,
    val hasKotlinAttribute: Boolean,
    val accessories: List<Accessory> = emptyList(),
)

fun interface AdmissionPolicy {
    fun admits(duck: Duck): Boolean
}

class KotlinOnly : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.hasKotlinAttribute
}

class MaxBudget(private val maxPrice: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.price <= maxPrice
}

class RequiresAccessory(private val accessoryName: String) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.accessories.any { it.name == accessoryName }
}

class MinAccessories(private val min: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.accessories.size >= min
}

class AllOf(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())
    override fun admits(duck: Duck): Boolean = policies.isNotEmpty() && policies.all { it.admits(duck) } // BUG: empty -> false
}

class AnyOf(private val policies: List<AdmissionPolicy>) : AdmissionPolicy {
    constructor(vararg policies: AdmissionPolicy) : this(policies.toList())
    override fun admits(duck: Duck): Boolean = policies.any { it.admits(duck) }
}

class Not(private val policy: AdmissionPolicy) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = !policy.admits(duck)
}
