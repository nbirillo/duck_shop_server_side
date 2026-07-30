package org.jetbrains.kotlin.course.duck.shop.admission

// demo-buggy: correct except for a single SEEDED bug in MaxBudget (`<` instead of `<=`), so
// the boundary test (price == maxPrice) fails while everything else passes. See agent.json.

class KotlinOnly : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.hasKotlinAttribute
}

class MaxBudget(private val maxPrice: Int) : AdmissionPolicy {
    // BUG: should be `<=`. Off-by-one on the boundary.
    override fun admits(duck: Duck): Boolean = duck.price < maxPrice
}

class RequiresAccessory(private val accessoryName: String) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.accessories.any { it.name == accessoryName }
}

class MinAccessories(private val min: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.accessories.size >= min
}
