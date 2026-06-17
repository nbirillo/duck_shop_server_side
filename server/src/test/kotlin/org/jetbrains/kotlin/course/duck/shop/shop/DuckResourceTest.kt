package org.jetbrains.kotlin.course.duck.shop.shop

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

/**
 * Web-layer integration test: the real Spring context + Spring Security, exercised through
 * MockMvc. @Transactional rolls back each test's DB changes for isolation (the seeded catalog,
 * committed at startup, stays).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DuckResourceTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `reading the shop is public`() {
        mockMvc.perform(get("/api/ducks")).andExpect(status().isOk)
    }

    @Test
    fun `mutating without credentials is 401`() {
        mockMvc.perform(post("/api/ducks")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `a user can add a duck`() {
        mockMvc.perform(post("/api/ducks").with(httpBasic("user", "user")))
            .andExpect(status().isCreated)
    }

    @Test
    fun `replacing the collection requires ADMIN`() {
        mockMvc.perform(put("/api/ducks").param("mode", "List").with(httpBasic("user", "user")))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `an admin replaces the collection and gets six ducks`() {
        mockMvc.perform(put("/api/ducks").param("mode", "List").with(httpBasic("admin", "admin")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(6))
    }

    @Test
    fun `an invalid mode returns the uniform 400 error body`() {
        mockMvc.perform(put("/api/ducks").param("mode", "Nope").with(httpBasic("admin", "admin")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
    }
}
