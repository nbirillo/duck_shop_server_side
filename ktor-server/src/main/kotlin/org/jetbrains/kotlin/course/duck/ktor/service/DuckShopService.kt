package org.jetbrains.kotlin.course.duck.ktor.service

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.kotlin.course.duck.ktor.db.DucksTable
import org.jetbrains.kotlin.course.duck.ktor.db.ShopItemsTable
import org.jetbrains.kotlin.course.duck.ktor.db.ShopStateTable
import org.jetbrains.kotlin.course.duck.ktor.domain.Duck
import org.jetbrains.kotlin.course.duck.ktor.domain.GameMode
import org.jetbrains.kotlin.course.duck.ktor.domain.JsDuck
import org.jetbrains.kotlin.course.duck.ktor.domain.MAX_NUMBER_OF_DUCKS
import org.jetbrains.kotlin.course.duck.ktor.domain.ShopStateResponse
import org.jetbrains.kotlin.course.duck.ktor.domain.toJsDuck

/**
 * The "shop window", backed by the database via Exposed transactions — the lightweight counterpart
 * to the Spring server's `@Transactional DuckShopService`. Each public method is one `transaction {}`.
 * Only the GET/PUT/POST subset is implemented (PATCH reorder and DELETE stay on the Spring side).
 */
class DuckShopService {

    /** Current collection as DTOs. */
    fun all(): List<JsDuck> = transaction {
        val mode = currentMode()
        currentDucks().map { it.toJsDuck(mode) }
    }

    /** Current mode + collection, so a client could restore its view on load. */
    fun state(): ShopStateResponse = transaction {
        val mode = currentMode()
        ShopStateResponse(mode, currentDucks().map { it.toJsDuck(mode) })
    }

    /** Replace the whole collection with a freshly generated one for the given mode (PUT, ADMIN only). */
    fun replaceWith(mode: GameMode): List<JsDuck> = transaction {
        setMode(mode)
        val catalog = catalogDucks()
        val generated = when (mode) {
            GameMode.List -> List(MAX_NUMBER_OF_DUCKS) { catalog.random() }
            GameMode.Set, GameMode.Map -> catalog.shuffled().take(MAX_NUMBER_OF_DUCKS)
        }
        rewrite(generated)
        generated.map { it.toJsDuck(mode) }
    }

    /** Add one random duck — unique in Set/Map modes, possibly a duplicate in List mode (POST). */
    fun addRandomDuck(): List<JsDuck> = transaction {
        val mode = currentMode()
        val current = currentDucks()
        val catalog = catalogDucks()
        val newDuck = when (mode) {
            GameMode.List -> catalog.random()
            GameMode.Set, GameMode.Map -> {
                val usedIds = current.map { it.id }.toSet()
                catalog.filter { it.id !in usedIds }.random()
            }
        }
        ShopItemsTable.insert {
            it[duckId] = newDuck.id
            it[position] = current.size
        }
        currentDucks().map { it.toJsDuck(mode) }
    }

    // ---- helpers (run inside an open transaction) ----

    private fun ResultRow.toDuck() = Duck(
        id = this[DucksTable.id],
        name = this[DucksTable.name],
        displayName = this[DucksTable.displayName],
        hasKotlinAttribute = this[DucksTable.hasKotlinAttribute],
    )

    private fun catalogDucks(): List<Duck> =
        DucksTable.selectAll().map { it.toDuck() }

    private fun currentDucks(): List<Duck> {
        val byId = catalogDucks().associateBy { it.id }
        return ShopItemsTable.selectAll()
            .orderBy(ShopItemsTable.position to SortOrder.ASC)
            .mapNotNull { byId[it[ShopItemsTable.duckId]] }
    }

    private fun currentMode(): GameMode =
        ShopStateTable.selectAll()
            .firstOrNull()
            ?.let { GameMode.valueOf(it[ShopStateTable.mode]) }
            ?: GameMode.List

    private fun setMode(mode: GameMode) {
        ShopStateTable.deleteWhere { ShopStateTable.id eq ShopStateTable.SINGLETON_ID }
        ShopStateTable.insert {
            it[id] = ShopStateTable.SINGLETON_ID
            it[ShopStateTable.mode] = mode.name
        }
    }

    /** Replace all shop items with the given ducks, preserving their order. */
    private fun rewrite(ducks: List<Duck>) {
        ShopItemsTable.deleteAll()
        ducks.forEachIndexed { i, duck ->
            ShopItemsTable.insert {
                it[duckId] = duck.id
                it[position] = i
            }
        }
    }
}
