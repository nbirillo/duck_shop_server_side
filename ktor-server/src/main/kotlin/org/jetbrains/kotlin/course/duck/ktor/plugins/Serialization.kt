package org.jetbrains.kotlin.course.duck.ktor.plugins

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

/**
 * JSON content negotiation via kotlinx.serialization (compare: Spring auto-configures Jackson).
 * The `@Serializable` annotation on the DTOs drives a compile-time serializer — no reflection.
 */
fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
}
