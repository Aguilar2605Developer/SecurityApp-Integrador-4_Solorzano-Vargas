package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.service.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class UserControllerTest {

    @Mock
    private lateinit var userService: UserService

    @InjectMocks
    private lateinit var userController: UserController

    private lateinit var sampleRequest: UserRequest
    private lateinit var sampleResponse: UserResponse

    @BeforeEach
    fun setUp() {
        sampleRequest = UserRequest(name = "Juan Perez", number = "0999999999")
        sampleResponse = UserResponse(id = 1L, name = "Juan Perez", email = "juan@example.com", number = "0999999999")
    }

    @AfterEach
    fun limpiarContexto() {
        SecurityContextHolder.clearContext()
    }

    private fun buildJwt(sub: String): Jwt {
        return Jwt.withTokenValue("token-de-prueba")
            .header("alg", "RS256")
            .claim("sub", sub)
            .claim("email", "juan@example.com")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
    }

    private fun autenticarComo(sub: String) {
        val jwt = buildJwt(sub)
        val auth = UsernamePasswordAuthenticationToken(jwt, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
    }

    // ---------------------- updateUser ----------------------

    @Test
    fun `updateUser deberia permitir la edicion cuando el usuario edita su propio perfil`() {
        autenticarComo("cognito-sub-1")
        whenever(userService.resolveLocalId("cognito-sub-1")).thenReturn(1L)
        whenever(userService.updateUser(1L, sampleRequest)).thenReturn(sampleResponse)

        val result = userController.updateUser(1L, sampleRequest)

        assertEquals(200, result.statusCode.value())
        verify(userService, times(1)).updateUser(1L, sampleRequest)
    }

    @Test
    fun `updateUser deberia lanzar AccessDenied cuando intenta editar el perfil de otro usuario`() {
        autenticarComo("cognito-sub-2")
        whenever(userService.resolveLocalId("cognito-sub-2")).thenReturn(2L)

        assertThrows(AccessDeniedException::class.java) {
            userController.updateUser(1L, sampleRequest)
        }
        verify(userService, never()).updateUser(any(), any())
    }

    @Test
    fun `updateUser deberia funcionar normalmente cuando no hay usuario autenticado en el contexto`() {
        SecurityContextHolder.clearContext()
        whenever(userService.resolveLocalId(null)).thenReturn(null)
        whenever(userService.updateUser(1L, sampleRequest)).thenReturn(sampleResponse)

        val result = userController.updateUser(1L, sampleRequest)

        assertEquals(200, result.statusCode.value())
        verify(userService, times(1)).updateUser(1L, sampleRequest)
    }

    // ---------------------- deleteUser ----------------------

    @Test
    fun `deleteUser deberia permitir eliminar cuando es su propia cuenta`() {
        autenticarComo("cognito-sub-1")
        whenever(userService.resolveLocalId("cognito-sub-1")).thenReturn(1L)
        whenever(userService.deleteUser(1L)).thenReturn(true)

        val result = userController.deleteUser(1L)

        assertEquals(204, result.statusCode.value())
        verify(userService, times(1)).deleteUser(1L)
    }

    @Test
    fun `deleteUser deberia lanzar AccessDenied cuando intenta eliminar la cuenta de otro usuario`() {
        autenticarComo("cognito-sub-2")
        whenever(userService.resolveLocalId("cognito-sub-2")).thenReturn(2L)

        assertThrows(AccessDeniedException::class.java) {
            userController.deleteUser(1L)
        }
        verify(userService, never()).deleteUser(any())
    }
}