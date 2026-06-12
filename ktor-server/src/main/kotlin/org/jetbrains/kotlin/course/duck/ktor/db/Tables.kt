package org.jetbrains.kotlin.course.duck.ktor.db

import org.jetbrains.exposed.sql.Table

/**
 * Exposed table definitions — the lightweight counterpart to the Spring server's JPA `@Entity`
 * classes. Note there is no many-to-many accessory table here: the Ktor port keeps the schema
 * minimal (accessories only mattered for price-sorting, which is part of the un-ported PATCH).
 */

/** The duck catalog (compare: `@Entity Duck` + `DuckRepository`). */
object DucksTable : Table("ducks") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 100)
    val displayName = varchar("display_name", 100).nullable()
    val hasKotlinAttribute = bool("has_kotlin_attribute").default(false)
    override val primaryKey = PrimaryKey(id)
}

/** One duck currently in the shop window, at a position (compare: `@Entity ShopItem`). */
object ShopItemsTable : Table("shop_items") {
    val id = long("id").autoIncrement()
    val duckId = long("duck_id").references(DucksTable.id)
    val position = integer("position")
    override val primaryKey = PrimaryKey(id)
}

/** Single-row table remembering the current mode across requests/restarts (compare: `@Entity ShopState`). */
object ShopStateTable : Table("shop_state") {
    val id = long("id")
    val mode = varchar("mode", 20)
    override val primaryKey = PrimaryKey(id)

    const val SINGLETON_ID = 1L
}
