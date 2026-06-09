package org.jetbrains.kotlin.course.duck.shop.duck

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

/** A duck accessory with a price. Shared across ducks (the inverse side of the relationship). */
@Entity
class Accessory(
    val name: String = "",
    val price: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
