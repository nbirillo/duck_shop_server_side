package org.jetbrains.kotlin.course.duck.ktor.routing

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.kotlin.course.duck.ktor.domain.toGameMode
import org.jetbrains.kotlin.course.duck.ktor.plugins.ApiError
import org.jetbrains.kotlin.course.duck.ktor.plugins.Role
import org.jetbrains.kotlin.course.duck.ktor.plugins.UserPrincipal
import org.jetbrains.kotlin.course.duck.ktor.service.DuckShopService

/**
 * The /api/ducks resource expressed with Ktor's routing DSL (compare: Spring's annotated
 * `@RestController DuckResource`). Only the GET/PUT/POST subset is ported.
 *
 *   GET  /api/ducks          read the collection            — public
 *   GET  /api/ducks/state    mode + collection              — public
 *   POST /api/ducks          add a random duck              — any authenticated user
 *   PUT  /api/ducks?mode=…   replace the whole collection   — ADMIN only (checked manually)
 */
fun Application.configureRouting(service: DuckShopService) {
    routing {
        route("/api/ducks") {
            get {
                call.respond(service.all())
            }
            get("/state") {
                call.respond(service.state())
            }

            authenticate("auth-basic") {
                post {
                    call.respond(HttpStatusCode.Created, service.addRandomDuck())
                }

                put {
                    // Ktor has no built-in role authorization — we check the role by hand.
                    val principal = call.principal<UserPrincipal>()
                    if (principal?.role != Role.ADMIN) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            ApiError(
                                HttpStatusCode.Forbidden.value,
                                HttpStatusCode.Forbidden.description,
                                "Requires the ADMIN role",
                            ),
                        )
                        return@put
                    }
                    val mode = (call.request.queryParameters["mode"]
                        ?: throw IllegalArgumentException("Missing 'mode' query parameter")).toGameMode()
                    call.respond(service.replaceWith(mode))
                }
            }
        }
    }
}
