package org.jetbrains.kotlin.course.duck.shop.shop

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.jetbrains.kotlin.course.duck.shop.duck.Duck

/** One duck currently in the shop window, at a given position. */
@Entity
class ShopItem(
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "duck_id")
    val duck: Duck = Duck(),
    var position: Int = 0,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
