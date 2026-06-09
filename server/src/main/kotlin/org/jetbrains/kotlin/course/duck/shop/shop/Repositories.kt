package org.jetbrains.kotlin.course.duck.shop.shop

import org.jetbrains.kotlin.course.duck.shop.duck.Accessory
import org.jetbrains.kotlin.course.duck.shop.duck.Duck
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DuckRepository : JpaRepository<Duck, Long>

@Repository
interface AccessoryRepository : JpaRepository<Accessory, Long>

@Repository
interface ShopItemRepository : JpaRepository<ShopItem, Long> {
    fun findAllByOrderByPositionAsc(): List<ShopItem>
}

@Repository
interface ShopStateRepository : JpaRepository<ShopState, Long>
