package com.pucetec.securitydev.security

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class CurrentUserTest {

    @AfterEach
    fun limpiar() {
        SecurityContextHolder.clearContext()
    }

    private fun buildJwt(sub: String, email: String?, name: String?): Jwt {
        val claims = mutableMapOf<String, Any>("sub" to sub)
        if (email != null) claims["email"] = email
        if (name != null) claims["name"] = name

        return Jwt.withTokenValue("token-de-prueba")
            .header("alg", "RS256")
            .claims { it.putAll(claims) }
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
    }

    @Test
    fun `sub deberia devolver null cuando no hay autenticacion`() {
        SecurityContextHolder.clearContext()
        assertNull(CurrentUser.sub())
    }

    @Test
    fun `sub deberia devolver null cuando el principal no es un Jwt`() {
        val auth = UsernamePasswordAuthenticationToken("test@example.com", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertNull(CurrentUser.sub())
    }

    @Test
    fun `sub deberia devolver el subject del Jwt`() {
        val jwt = buildJwt(sub = "cognito-sub-123", email = "test@example.com", name = "Test User")
        val auth = UsernamePasswordAuthenticationToken(jwt, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertEquals("cognito-sub-123", CurrentUser.sub())
    }

    @Test
    fun `email deberia devolver el claim email del Jwt`() {
        val jwt = buildJwt(sub = "cognito-sub-123", email = "test@example.com", name = "Test User")
        val auth = UsernamePasswordAuthenticationToken(jwt, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertEquals("test@example.com", CurrentUser.email())
    }

    @Test
    fun `name deberia devolver el claim name del Jwt`() {
        val jwt = buildJwt(sub = "cognito-sub-123", email = "test@example.com", name = "Test User")
        val auth = UsernamePasswordAuthenticationToken(jwt, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        assertEquals("Test User", CurrentUser.name())
    }
}