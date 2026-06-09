package org.jetbrains.kotlin.course.duck.shop.shop

import org.jetbrains.kotlin.course.duck.shop.utils.GameMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Integration test against a real PostgreSQL started by Testcontainers.
 * Skipped automatically when Docker isn't available (disabledWithoutDocker = true).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class DuckShopPersistenceTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var duckRepository: DuckRepository

    @Autowired
    lateinit var shop: DuckShopService

    @Test
    fun `catalog is seeded with ten ducks`() {
        assertEquals(10, duckRepository.count())
    }

    @Test
    fun `the duck-to-accessory relationship is persisted`() {
        val alex = duckRepository.findAll().first { it.name == "Alex" }
        assertEquals(setOf("Monocle", "Hat"), alex.accessories.map { it.name }.toSet())
    }

    @Test
    fun `the shop window survives through the database`() {
        shop.replaceWith(GameMode.List)
        assertEquals(6, shop.all().size)
        shop.addRandomDuck()
        assertEquals(7, shop.all().size)
    }
}
