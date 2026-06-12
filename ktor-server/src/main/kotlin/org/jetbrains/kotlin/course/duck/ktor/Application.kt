package org.jetbrains.kotlin.course.duck.ktor

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import org.jetbrains.kotlin.course.duck.ktor.db.initDatabase
import org.jetbrains.kotlin.course.duck.ktor.plugins.configureSecurity
import org.jetbrains.kotlin.course.duck.ktor.plugins.configureSerialization
import org.jetbrains.kotlin.course.duck.ktor.plugins.configureStatusPages
import org.jetbrains.kotlin.course.duck.ktor.routing.configureRouting
import org.jetbrains.kotlin.course.duck.ktor.service.DuckShopService

/** The Ktor "comparison" duckShop. API-only, runs on :8081 next to the Spring server (:8080). */
fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    initDatabase()

    install(CallLogging)
    install(CORS) {
        allowHost("localhost:3000") // the dev frontend (compare: Spring's CorsConfigurationSource)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
    }

    configureSerialization()
    configureSecurity()
    configureStatusPages()
    configureRouting(DuckShopService())
}
