package org.jetbrains.kotlin.course.duck.shop.admission

// Leaf policies — the atomic specifications. Bodies are TODO(): this is the test-first
// "red" state a student (or an AI agent) fills in until :starter:test goes green.

/**
 * Admits only ducks that carry the Kotlin attribute.
 *
 * `admits(duck)` is `true` iff [Duck.hasKotlinAttribute] is `true`.
 */
class KotlinOnly : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = TODO("implement KotlinOnly")
}

/**
 * Admits ducks whose price does not exceed [maxPrice].
 *
 * `admits(duck)` is `true` iff `duck.price <= maxPrice`.
 */
class MaxBudget(private val maxPrice: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = TODO("implement MaxBudget")
}

/**
 * Admits ducks that wear at least one accessory named [accessoryName].
 *
 * `admits(duck)` is `true` iff some accessory in [Duck.accessories] has that name.
 */
class RequiresAccessory(private val accessoryName: String) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = TODO("implement RequiresAccessory")
}

/**
 * Admits ducks wearing at least [min] accessories.
 *
 * `admits(duck)` is `true` iff `duck.accessories.size >= min`.
 */
class MinAccessories(private val min: Int) : AdmissionPolicy {
    override fun admits(duck: Duck): Boolean = TODO("implement MinAccessories")
}
