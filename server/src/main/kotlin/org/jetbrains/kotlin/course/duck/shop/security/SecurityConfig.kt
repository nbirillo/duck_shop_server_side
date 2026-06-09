package org.jetbrains.kotlin.course.duck.shop.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Security rules for the duck-shop API:
 *  - reading the shop (GET) is public, as are the SPA, actuator and the H2 console;
 *  - changing the collection (POST / PATCH / DELETE) requires an authenticated user;
 *  - replacing the whole collection (PUT) requires the ADMIN role.
 *
 * Authentication is HTTP Basic against an in-memory user store (dev only).
 */
@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors(Customizer.withDefaults())
            .csrf { it.disable() } // stateless API consumed by non-browser clients too
            .headers { it.frameOptions { frame -> frame.disable() } } // allow the H2 console (dev)
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.PUT, "/api/ducks").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/ducks").authenticated()
                    .requestMatchers(HttpMethod.PATCH, "/api/ducks").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/ducks", "/api/ducks/**").authenticated()
                    .anyRequest().permitAll() // SPA, GET API, /actuator/**, /h2-console/**
            }
            .httpBasic(Customizer.withDefaults())
        return http.build()
    }

    @Bean
    fun userDetailsService(encoder: PasswordEncoder): UserDetailsService {
        val user = User.withUsername("user").password(encoder.encode("user")).roles("USER").build()
        val admin = User.withUsername("admin").password(encoder.encode("admin")).roles("ADMIN").build()
        return InMemoryUserDetailsManager(user, admin)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf("http://localhost:3000") // the dev frontend (npm start)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }
}
