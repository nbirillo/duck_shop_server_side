package org.jetbrains.kotlin.course.duck.shop.admission

/**
 * An accessory a duck can wear (e.g. a hat, a scarf).
 *
 * @property name the accessory's name; used by [RequiresAccessory].
 */
data class Accessory(val name: String)

/**
 * A duck being considered for admission to a shop.
 *
 * This is the minimal, self-contained domain used by the in-memory MVP — it deliberately
 * does not depend on the upstream course's JPA entities.
 *
 * @property name the duck's display name.
 * @property price the duck's price, in whole currency units; used by [MaxBudget].
 * @property hasKotlinAttribute whether the duck carries the "Kotlin" attribute; used by [KotlinOnly].
 * @property accessories the accessories the duck currently wears (may be empty).
 */
data class Duck(
    val name: String,
    val price: Int,
    val hasKotlinAttribute: Boolean,
    val accessories: List<Accessory> = emptyList(),
)
