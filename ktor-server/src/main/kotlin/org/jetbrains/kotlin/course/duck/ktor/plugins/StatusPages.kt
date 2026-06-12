package org.jetbrains.kotlin.course.duck.ktor.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable

/** Uniform JSON error body — the Ktor counterpart to the Spring server's `ApiError`. */
@Serializable
data class ApiError(val status: Int, val error: String, val message: String?)

/**
 * Centralised error handling (compare: Spring's `@RestControllerAdvice`). An unknown `mode` raises
 * IllegalArgumentException → 400 with a consistent JSON shape.
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(HttpStatusCode.BadRequest.value, HttpStatusCode.BadRequest.description, cause.message),
            )
        }
    }
}
