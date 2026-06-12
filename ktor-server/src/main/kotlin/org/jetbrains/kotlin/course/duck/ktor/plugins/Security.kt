package org.jetbrains.kotlin.course.duck.ktor.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic

/** Roles, mirroring the Spring server's USER / ADMIN authorities. */
enum class Role { USER, ADMIN }

/** What ends up in `call.principal<UserPrincipal>()` after successful Basic auth. */
data class UserPrincipal(val name: String, val role: Role)

private data class Account(val password: String, val role: Role)

/**
 * In-memory user store for the lecture (compare: Spring's `InMemoryUserDetailsManager`).
 * Passwords are kept in plain text here for simplicity — the Spring side uses BCrypt; call that
 * difference out on the slide.
 */
private val accounts = mapOf(
    "user" to Account("user", Role.USER),
    "admin" to Account("admin", Role.ADMIN),
)

/**
 * HTTP Basic authentication (compare: Spring Security `httpBasic`). Note Ktor authenticates here
 * but does NOT do role-based authorization — there is no built-in `hasRole`. The PUT route checks
 * the role manually (see DuckRoutes), which is the key contrast with Spring's `SecurityFilterChain`.
 */
fun Application.configureSecurity() {
    install(Authentication) {
        basic("auth-basic") {
            realm = "duck-shop"
            validate { credentials ->
                val account = accounts[credentials.name]
                if (account != null && account.password == credentials.password) {
                    UserPrincipal(credentials.name, account.role)
                } else {
                    null
                }
            }
        }
    }
}
