package org.jetbrains.kotlin.course.duck.shop.admission

// Reference implementation of the leaf policies (teacher-only, :grading). Mirrors the specs
// in the :starter stubs; kept here so the student's project never contains the answers.

/**
 * Admits only ducks that carry the Kotlin attribute.
 *
 * `admits(duck)` is `true` iff [Duck.hasKotlinAttribute] is `true`.
 */
class KotlinOnly : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.hasKotlinAttribute
}

/**
 * Admits ducks whose price does not exceed [maxPrice].
 *
 * `admits(duck)` is `true` iff `duck.price <= maxPrice`.
 */
class MaxBudget(private val maxPrice: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.price <= maxPrice
}

/**
 * Admits ducks that wear at least one accessory named [accessoryName].
 *
 * `admits(duck)` is `true` iff some accessory in [Duck.accessories] has that name.
 */
class RequiresAccessory(private val accessoryName: String) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.accessories.any { it.name == accessoryName }
}

/**
 * Admits ducks wearing at least [min] accessories.
 *
 * `admits(duck)` is `true` iff `duck.accessories.size >= min`.
 */
class MinAccessories(private val min: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = duck.accessories.size >= min
}
