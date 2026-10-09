package org.jetbrains.kotlin.course.duck.shop.admission

import org.jetbrains.kotlin.course.duck.shop.pricing.DiscountRule

/**
 * An accessory that a duck can wear (e.g. a hat, a scarf).
 *
 * @property name the name of the accessory; used by [RequiresAccessory].
 */
data class Accessory(val name: String)

/**
 * A duck being considered for entry to a shop.
 *
 * This is the minimal, self-contained domain for the in-memory MVP — it intentionally
 * does not depend on the upstream course's JPA entities.
 *
 * @property name the name displayed for the duck.
 * @property price the duck's price, in whole currency units; used by [MaxBudget].
 * @property hasKotlinAttribute whether the duck has the "Kotlin" attribute; used by [KotlinOnly].
 * @property accessories the accessories the duck is currently wearing (may be empty).
 */
data class Duck(
    val name: String,
    val price: Int,
    val hasKotlinAttribute: Boolean,
    val accessories: List<Accessory> = emptyList(),
)

/**
 * A duck shop that uses its [admissionPolicy] to decide which ducks to admit.
 *
 * @property name the name of the shop.
 * @property admissionPolicy the policy the shop applies to candidate ducks.
 */
data class Shop(
    val name: String,
    val admissionPolicy: AdmissionPolicy,
    /**
     * The promotions offered by this shop. This has a default value, so all existing uses of
     * `Shop(name, policy)` — throughout the test-writing exercise — remain unchanged.
     */
    val promotions: List<DiscountRule> = emptyList(),
) {
    /** Returns `true` if [admissionPolicy] allows [duck] into this shop. */
    fun admits(duck: Duck): Boolean = admissionPolicy.admits(duck)
}
