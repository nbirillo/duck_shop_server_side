package org.jetbrains.kotlin.course.duck.shop.shop

import org.jetbrains.kotlin.course.duck.shop.duck.Duck
import org.jetbrains.kotlin.course.duck.shop.duck.JsDuck
import org.jetbrains.kotlin.course.duck.shop.functions.action.GameActionFunctionsService
import org.jetbrains.kotlin.course.duck.shop.mode.GameModeService
import org.jetbrains.kotlin.course.duck.shop.utils.GameMode
import org.jetbrains.kotlin.course.duck.shop.utils.toJsDuck
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The "shop window" — the current duck collection, now backed by the database
 * (resource: /api/ducks). Every operation is a transactional DB change, so the collection
 * survives restarts and is no longer held in memory.
 */
@Service
class DuckShopService(
    private val modeService: GameModeService,
    private val actionService: GameActionFunctionsService,
    private val duckRepository: DuckRepository,
    private val shopItemRepository: ShopItemRepository,
    private val shopStateRepository: ShopStateRepository,
) {
    /** Replace the whole collection with a freshly generated one for the given mode. */
    @Transactional
    fun replaceWith(mode: GameMode): List<JsDuck> {
        shopStateRepository.save(ShopState(mode = mode))
        val generated = when (mode) {
            GameMode.List -> modeService.generateListOfDucks()
            GameMode.Set, GameMode.Map -> modeService.generateUniqueDucks()
        }
        rewrite(generated)
        return snapshot()
    }

    @Transactional(readOnly = true)
    fun all(): List<JsDuck> = snapshot()

    /** Current mode + collection, so the client can restore its view on load. */
    @Transactional(readOnly = true)
    fun state(): ShopStateResponse = ShopStateResponse(currentMode(), snapshot())

    /** Add one random duck (unique in Set/Map modes, possibly duplicate in List mode). */
    @Transactional
    fun addRandomDuck(): List<JsDuck> {
        val current = currentDucks()
        val catalog = duckRepository.findAll()
        val newDuck = when (currentMode()) {
            GameMode.List -> catalog.random()
            GameMode.Set, GameMode.Map -> {
                val usedIds = current.mapNotNull { it.id }.toSet()
                catalog.filter { it.id !in usedIds }.random()
            }
        }
        shopItemRepository.save(ShopItem(duck = newDuck, position = current.size))
        return snapshot()
    }

    /** Remove the duck at the given position. */
    @Transactional
    fun removeAt(index: Int): List<JsDuck> {
        val ducks = currentDucks()
        require(index in ducks.indices) { "No duck at index $index" }
        rewrite(ducks.filterIndexed { i, _ -> i != index })
        return snapshot()
    }

    /** Delete every duck that doesn't have the Kotlin attribute (the "filter" operation). */
    @Transactional
    fun removeWithoutKotlin(): List<JsDuck> = with(actionService) {
        rewrite(currentDucks().deleteDucksWithoutKotlinStuff())
        snapshot()
    }

    /** Reorder the collection (sort by price / shuffle / Kotlin-first). */
    @Transactional
    fun reorder(order: Order): List<JsDuck> = with(actionService) {
        val ducks = currentDucks()
        val reordered = when (order) {
            Order.PRICE -> ducks.sortDucks()
            Order.RANDOM -> ducks.shuffleDucks()
            Order.KOTLIN_FIRST -> ducks.divideDucksIntoKotlinAndNonKotlin()
                .let { (withKotlin, withoutKotlin) -> withKotlin + withoutKotlin }
        }
        rewrite(reordered)
        snapshot()
    }

    private fun currentMode(): GameMode =
        shopStateRepository.findById(ShopState.SINGLETON_ID).map { it.mode }.orElse(GameMode.List)

    private fun currentDucks(): List<Duck> =
        shopItemRepository.findAllByOrderByPositionAsc().map { it.duck }

    /** Replace all shop items with the given ducks, preserving their order. */
    private fun rewrite(ducks: List<Duck>) {
        shopItemRepository.deleteAll()
        shopItemRepository.flush()
        ducks.forEachIndexed { i, duck -> shopItemRepository.save(ShopItem(duck = duck, position = i)) }
    }

    private fun snapshot(): List<JsDuck> {
        val mode = currentMode()
        return currentDucks().map { it.toJsDuck(mode) }
    }
}
