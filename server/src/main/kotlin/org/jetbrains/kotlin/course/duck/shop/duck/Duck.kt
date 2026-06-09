package org.jetbrains.kotlin.course.duck.shop.duck

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany

/**
 * A duck from the catalog. Each duck has a set of accessories — a many-to-many relationship
 * (an accessory like "Hat" can belong to several ducks).
 */
@Entity
class Duck(
    val name: String = "",
    val displayName: String? = null,
    val hasKotlinAttribute: Boolean = false,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "duck_accessories",
        joinColumns = [JoinColumn(name = "duck_id")],
        inverseJoinColumns = [JoinColumn(name = "accessory_id")],
    )
    val accessories: Set<Accessory> = emptySet(),
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
)
