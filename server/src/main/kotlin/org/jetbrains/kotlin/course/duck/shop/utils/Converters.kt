package org.jetbrains.kotlin.course.duck.shop.utils

import org.jetbrains.kotlin.course.duck.shop.duck.JsDuck
import org.jetbrains.kotlin.course.duck.shop.duck.Duck

fun String.toGameMode(): GameMode =
    GameMode.entries.find { it.name == this } ?: throw IllegalArgumentException("Unknown mode: $this")

fun Duck.getJsDescription(mode: GameMode): String? = when (mode) {
    GameMode.Map -> this.displayName ?: this.name
    else -> null
}

fun Duck.toJsDuck(mode: GameMode): JsDuck =
    JsDuck(this.displayName ?: this.name, hasKotlinAttribute = this.hasKotlinAttribute, description = this.getJsDescription(mode))
