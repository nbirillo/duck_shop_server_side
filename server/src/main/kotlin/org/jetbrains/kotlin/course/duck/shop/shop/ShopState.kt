package org.jetbrains.kotlin.course.duck.shop.shop

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.jetbrains.kotlin.course.duck.shop.utils.GameMode

/** Single-row entity that remembers the current collection mode across requests/restarts. */
@Entity
class ShopState(
    @Enumerated(EnumType.STRING)
    var mode: GameMode = GameMode.List,
    @Id
    val id: Long = SINGLETON_ID,
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}
