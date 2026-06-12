package org.jetbrains.kotlin.course.duck.ktor.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Connects to a file-based H2 database (so the shop survives restarts, like the Spring server),
 * creates the schema, and seeds the duck catalog once if it is empty.
 *
 * The H2 file is separate from the Spring server's (`./data/duckshop-ktor`) so the two apps can
 * run side by side during the lecture without clobbering each other's data.
 */
fun initDatabase() {
    Database.connect(
        url = "jdbc:h2:file:./data/duckshop-ktor;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver",
    )
    transaction {
        SchemaUtils.create(DucksTable, ShopItemsTable, ShopStateTable)
        seedCatalogIfEmpty()
    }
}

/** The same 10 ducks as the Spring server's `CatalogSeeder` (without accessories). */
private fun seedCatalogIfEmpty() {
    if (DucksTable.selectAll().any()) return

    val catalog = listOf(
        Triple("Alex", null, false),
        Triple("Daniel", null, true),
        Triple("Dorian", null, false),
        Triple("Jack", null, false),
        Triple("Kristian", null, false),
        Triple("Leo", null, false),
        Triple("MrPink", "Mr. Pink", false),
        Triple("Oliver", null, false),
        Triple("Piter", null, false),
        Triple("Vanessa", null, true),
    )
    catalog.forEach { (duckName, duckDisplayName, kotlin) ->
        DucksTable.insert {
            it[name] = duckName
            it[displayName] = duckDisplayName
            it[hasKotlinAttribute] = kotlin
        }
    }
}
