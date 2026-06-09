package org.jetbrains.kotlin.course.duck.shop.shop

import org.jetbrains.kotlin.course.duck.shop.duck.JsDuck
import org.jetbrains.kotlin.course.duck.shop.utils.GameMode
import org.jetbrains.kotlin.course.duck.shop.utils.toGameMode
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * REST resource for the duck collection (the "shop window").
 * One resource — /api/ducks — exercises all five HTTP methods:
 *   GET    /api/ducks                 read the collection
 *   PUT    /api/ducks?mode=List       replace it with a fresh collection
 *   POST   /api/ducks                 add a duck
 *   PATCH  /api/ducks  {order:...}    reorder (sort / shuffle / kotlin-first)
 *   DELETE /api/ducks/{index}         remove one duck
 *   DELETE /api/ducks?hasKotlin=false remove all non-Kotlin ducks (filter)
 */
@RestController
@RequestMapping("/api/ducks")
class DuckResource(private val shop: DuckShopService) {
    
    @GetMapping
    fun getDucks(): List<JsDuck> = shop.all()

    /** Current collection plus its mode — lets the client restore state on page load. */
    @GetMapping("/state")
    fun getState(): ShopStateResponse = shop.state()

    @PutMapping
    fun replaceCollection(@RequestParam mode: String): List<JsDuck> =
        shop.replaceWith(mode.toGameMode())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addDuck(): List<JsDuck> = shop.addRandomDuck()

    @PatchMapping
    fun reorderDucks(@RequestBody request: ReorderRequest): List<JsDuck> =
        shop.reorder(request.order.toOrder())

    @DeleteMapping("/{index}")
    fun removeDuck(@PathVariable index: Int): List<JsDuck> =
        try {
            shop.removeAt(index)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
        }

    @DeleteMapping(params = ["hasKotlin"])
    fun filterDucks(@RequestParam hasKotlin: Boolean): List<JsDuck> =
        shop.removeWithoutKotlin()
}

data class ReorderRequest(val order: String)

/** Snapshot of the shop window for restoring client state: the mode and the current ducks. */
data class ShopStateResponse(val mode: GameMode, val ducks: List<JsDuck>)

enum class Order(val value: String) {
    PRICE("price"),
    RANDOM("random"),
    KOTLIN_FIRST("kotlin-first"),
    ;
}

fun String.toOrder(): Order =
    Order.entries.find { it.value == this }
        ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown order: $this")
