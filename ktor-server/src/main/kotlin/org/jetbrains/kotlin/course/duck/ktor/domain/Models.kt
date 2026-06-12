package org.jetbrains.kotlin.course.duck.ktor.domain

import kotlinx.serialization.Serializable

/** How many ducks a freshly generated shop window holds (mirrors the Spring server). */
const val MAX_NUMBER_OF_DUCKS = 6

/**
 * The DTO sent to clients — identical shape to the Spring server's `JsDuck`, but here it is a
 * kotlinx.serialization `@Serializable` class (Spring relies on Jackson reflection instead).
 */
@Serializable
data class JsDuck(val name: String, val description: String?, val hasKotlinAttribute: Boolean)

/** The mode the shop window was generated in. List allows duplicates; Set/Map are unique. */
@Serializable
enum class GameMode { List, Set, Map }

fun String.toGameMode(): GameMode =
    GameMode.entries.find { it.name == this } ?: throw IllegalArgumentException("Unknown mode: $this")

/** A duck from the catalog (no accessories here — the Ktor port keeps persistence deliberately simple). */
data class Duck(
    val id: Long,
    val name: String,
    val displayName: String?,
    val hasKotlinAttribute: Boolean,
)

/** In Map mode the description is the (display) name; otherwise it is absent (mirror of Converters.kt). */
fun Duck.toJsDuck(mode: GameMode): JsDuck =
    JsDuck(
        name = displayName ?: name,
        description = if (mode == GameMode.Map) displayName ?: name else null,
        hasKotlinAttribute = hasKotlinAttribute,
    )

/** Snapshot of the shop window — the current mode plus its ducks. Mirrors `ShopStateResponse`. */
@Serializable
data class ShopStateResponse(val mode: GameMode, val ducks: List<JsDuck>)
