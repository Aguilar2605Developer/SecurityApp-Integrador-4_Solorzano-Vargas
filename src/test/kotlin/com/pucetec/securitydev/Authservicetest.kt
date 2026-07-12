package com.pucetec.securitydev

import com.pucetec.securitydev.dto.AuthRequest
import com.pucetec.securitydev.service.AuthService
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.UserRepository
import com.pucetec.securitydev.security.JwtUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.util.ReflectionTestUtils

/**
 * Tests unitarios para AuthService.
 *
 * NOTA: `adminEmail` se inyecta con @Value en tiempo de ejecución de Spring,
 * pero en un test unitario (sin contexto de Spring) no se resuelve solo.
 * Por eso se setea manualmente con ReflectionTestUtils en el setUp().
 *
 * Ajusta el paquete de `Users` si difiere del real (com.pucetec.securitydev.entity.Users).
 */
@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var jwtUtil: JwtUtil

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var authService: AuthService

    private val adminEmail = "admin@pucetec.com"

    private lateinit var normalUser: Users
    private lateinit var adminUser: Users

    @BeforeEach
    fun setUp() {
        authService = AuthService(userRepository, jwtUtil, passwordEncoder)
        // Inyecta manualmente el valor de @Value("${app.admin.email}") en el campo privado
        ReflectionTestUtils.setField(authService, "adminEmail", adminEmail)

        normalUser = Users(
            id = 1L,
            name = "Juan Perez",
            email = "juan@example.com",
            number = "0999999999",
            password = "encodedPass",
            hotSpots = mutableListOf()
        )

        adminUser = Users(
            id = 2L,
            name = "Admin User",
            email = adminEmail,
            number = "0988888888",
            password = "encodedAdminPass",
            hotSpots = mutableListOf()
        )
    }

    // ── login: casos exitosos ───────────────────────────────────────

    @Test
    fun `login deberia retornar AuthResponse cuando credenciales son correctas`() {
        val request = AuthRequest(email = "juan@example.com", password = "plainPass")

        whenever(userRepository.findByEmail(request.email)).doReturn(normalUser)
        whenever(passwordEncoder.matches(request.password, normalUser.password)).doReturn(true)
        whenever(jwtUtil.generateToken(normalUser.email, normalUser.id)).doReturn("fake-jwt-token")

        val result = authService.login(request)

        assertNotNull(result)
        assertEquals("fake-jwt-token", result!!.token)
        assertEquals(normalUser.id, result.userId)
        assertEquals(normalUser.name, result.name)
        assertFalse(result.isAdmin)
    }

    @Test
    fun `login deberia marcar isAdmin en true cuando el email coincide con adminEmail`() {
        val request = AuthRequest(email = adminEmail, password = "plainAdminPass")

        whenever(userRepository.findByEmail(request.email)).doReturn(adminUser)
        whenever(passwordEncoder.matches(request.password, adminUser.password)).doReturn(true)
        whenever(jwtUtil.generateToken(adminUser.email, adminUser.id)).doReturn("fake-admin-token")

        val result = authService.login(request)

        assertNotNull(result)
        assertTrue(result!!.isAdmin)
    }

    @Test
    fun `login deberia ignorar mayusculas y minusculas al comparar con adminEmail`() {
        val request = AuthRequest(email = "ADMIN@PUCETEC.COM", password = "plainAdminPass")
        val userConMayusculas = Users(
            id = adminUser.id,
            name = adminUser.name,
            email = "ADMIN@PUCETEC.COM",
            number = adminUser.number,
            password = adminUser.password,
            hotSpots = adminUser.hotSpots
        )

        whenever(userRepository.findByEmail(request.email)).doReturn(userConMayusculas)
        whenever(passwordEncoder.matches(request.password, userConMayusculas.password)).doReturn(true)
        whenever(jwtUtil.generateToken(userConMayusculas.email, userConMayusculas.id)).doReturn("token")

        val result = authService.login(request)

        assertNotNull(result)
        assertTrue(result!!.isAdmin)
    }

    // ── login: casos fallidos ───────────────────────────────────────

    @Test
    fun `login deberia retornar null si el usuario no existe`() {
        val request = AuthRequest(email = "noexiste@example.com", password = "cualquierPass")

        whenever(userRepository.findByEmail(request.email)).doReturn(null)

        val result = authService.login(request)

        assertNull(result)
        verify(passwordEncoder, never()).matches(org.mockito.kotlin.any(), org.mockito.kotlin.any())
        verify(jwtUtil, never()).generateToken(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    @Test
    fun `login deberia retornar null si la contrasena es incorrecta`() {
        val request = AuthRequest(email = "juan@example.com", password = "passIncorrecta")

        whenever(userRepository.findByEmail(request.email)).doReturn(normalUser)
        whenever(passwordEncoder.matches(request.password, normalUser.password)).doReturn(false)

        val result = authService.login(request)

        assertNull(result)
        verify(jwtUtil, never()).generateToken(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }
}