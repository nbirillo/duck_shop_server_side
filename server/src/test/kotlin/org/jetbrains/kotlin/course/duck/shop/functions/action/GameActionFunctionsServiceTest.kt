package org.jetbrains.kotlin.course.duck.shop.functions.action

import org.jetbrains.kotlin.course.duck.shop.duck.Accessory
import org.jetbrains.kotlin.course.duck.shop.duck.Duck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Pure unit tests for the collection operations — no Spring, no database. */
class GameActionFunctionsServiceTest {

    private val service = GameActionFunctionsService()

    private fun duck(name: String, kotlin: Boolean, vararg prices: Int) =
        Duck(
            name = name,
            hasKotlinAttribute = kotlin,
            accessories = prices.map { Accessory("acc-$it", it) }.toSet(),
        )

    @Test
    fun `sort puts kotlin ducks first thanks to the 100x coefficient`() = with(service) {
        val plainExpensive = duck("Plain", kotlin = false, 110) // score 110
        val kotlinCheap = duck("Kot", kotlin = true, 30)         // score 30 * 100 = 3000
        val sorted = listOf(plainExpensive, kotlinCheap).sortDucks()
        assertEquals(listOf("Kot", "Plain"), sorted.map { it.name })
    }

    @Test
    fun `filter keeps only ducks with the kotlin attribute`() = with(service) {
        val ducks = listOf(duck("A", true, 10), duck("B", false, 10), duck("C", true, 5))
        assertEquals(listOf("A", "C"), ducks.deleteDucksWithoutKotlinStuff().map { it.name })
    }

    @Test
    fun `partition splits kotlin from non-kotlin`() = with(service) {
        val ducks = listOf(duck("A", true), duck("B", false), duck("C", true))
        val (withKotlin, withoutKotlin) = ducks.divideDucksIntoKotlinAndNonKotlin()
        assertEquals(listOf("A", "C"), withKotlin.map { it.name })
        assertEquals(listOf("B"), withoutKotlin.map { it.name })
    }

    @Test
    fun `shuffle keeps exactly the same elements`() = with(service) {
        val ducks = listOf(duck("A", true), duck("B", false), duck("C", true))
        assertEquals(ducks.toSet(), ducks.shuffleDucks().toSet())
    }
}
