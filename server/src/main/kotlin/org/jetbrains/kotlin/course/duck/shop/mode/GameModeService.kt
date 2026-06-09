package org.jetbrains.kotlin.course.duck.shop.mode

import org.jetbrains.kotlin.course.duck.shop.duck.Duck
import org.jetbrains.kotlin.course.duck.shop.shop.DuckRepository
import org.jetbrains.kotlin.course.duck.shop.utils.MAX_NUMBER_OF_DUCKS
import org.springframework.stereotype.Service

/** Generates a fresh collection of ducks from the catalog stored in the database. */
@Service
class GameModeService(private val duckRepository: DuckRepository) {

    /** A list may contain duplicates. */
    fun generateListOfDucks(): List<Duck> {
        val catalog = duckRepository.findAll()
        return List(MAX_NUMBER_OF_DUCKS) { catalog.random() }
    }

    /** Sets and maps hold unique ducks. */
    fun generateUniqueDucks(): List<Duck> =
        duckRepository.findAll().shuffled().take(MAX_NUMBER_OF_DUCKS)
}
