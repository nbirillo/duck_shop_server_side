package org.jetbrains.kotlin.course.duck.shop.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

/** A single, consistent JSON shape for every error the API returns. */
data class ApiError(val status: Int, val error: String, val message: String?)

/**
 * Centralized error handling for the whole API. Turns exceptions into a uniform JSON body
 * instead of Spring's default error page, so clients always get the same structure.
 */
@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> {
        val status = HttpStatus.valueOf(ex.statusCode.value())
        return ResponseEntity.status(status).body(ApiError(status.value(), status.reasonPhrase, ex.reason))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiError> {
        val status = HttpStatus.BAD_REQUEST
        return ResponseEntity.status(status).body(ApiError(status.value(), status.reasonPhrase, ex.message))
    }
}
