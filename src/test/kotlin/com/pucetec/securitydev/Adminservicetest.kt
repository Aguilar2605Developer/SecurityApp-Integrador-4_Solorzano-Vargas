package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserCreateRequest
import com.pucetec.securitydev.dto.UserUpdateRequest
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

/**
 * Tests unitarios para AdminService.
 *
 * Requiere en build.gradle.kts (o pom.xml):
 *   testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
 *   testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
 *   testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
 *
 * NOTA: Ajusta los nombres/campos de las entidades y DTOs (Users, HotSpot,
 * UserCreateRequest, UserUpdateRequest, UserAdminResponse, DashboardResponse)
 * si difieren de los que se infirieron a partir de AdminService.
 */
@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var hotSpotRepository: HotSpotRepository

    @Mock
    lateinit var locationShareRepository: LocationShareRepository

    @Mock
    lateinit var passwordEncoder: PasswordEncoder

    private lateinit var adminService: AdminService

    private lateinit var sampleUser: Users

    @BeforeEach
    fun setUp() {
        adminService = AdminService(
            userRepository,
            hotSpotRepository,
            locationShareRepository,
            passwordEncoder
        )

        sampleUser = Users(
            id = 1L,
            name = "Juan Perez",
            email = "juan@example.com",
            number = "0999999999",
            password = "encodedPass",
            hotSpots = mutableListOf()
        )
    }

    // ── getAllUsers ─────────────────────────────────────────────────

    @Test
    fun `getAllUsers deberia retornar la lista mapeada de usuarios`() {
        whenever(userRepository.findAll()).doReturn(listOf(sampleUser))

        val result = adminService.getAllUsers()

        assertEquals(1, result.size)
        assertEquals(sampleUser.id, result[0].id)
        assertEquals(sampleUser.email, result[0].email)
        verify(userRepository, times(1)).findAll()
    }

    @Test
    fun `getAllUsers deberia retornar lista vacia si no hay usuarios`() {
        whenever(userRepository.findAll()).doReturn(emptyList())

        val result = adminService.getAllUsers()

        assertTrue(result.isEmpty())
    }

    // ── getUserById ─────────────────────────────────────────────────

    @Test
    fun `getUserById deberia retornar el usuario si existe`() {
        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))

        val result = adminService.getUserById(1L)

        assertEquals(sampleUser.id, result.id)
        assertEquals(sampleUser.name, result.name)
    }

    @Test
    fun `getUserById deberia lanzar excepcion si el usuario no existe`() {
        whenever(userRepository.findById(99L)).doReturn(Optional.empty())

        val exception = assertThrows(RuntimeException::class.java) {
            adminService.getUserById(99L)
        }
        assertTrue(exception.message!!.contains("99"))
    }

    // ── createUser ──────────────────────────────────────────────────

    @Test
    fun `createUser deberia crear el usuario cuando el correo no existe`() {
        val request = UserCreateRequest(
            name = "Nuevo Usuario",
            email = "nuevo@example.com",
            number = "0988888888",
            password = "plainPass"
        )

        whenever(userRepository.existsByEmail(request.email)).doReturn(false)
        whenever(passwordEncoder.encode(request.password)).doReturn("encodedPlainPass")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as Users }

        val result = adminService.createUser(request)

        assertEquals(request.name, result.name)
        assertEquals(request.email, result.email)
        verify(passwordEncoder, times(1)).encode(request.password)
        verify(userRepository, times(1)).save(any())
    }

    @Test
    fun `createUser deberia lanzar excepcion si el correo ya existe`() {
        val request = UserCreateRequest(
            name = "Duplicado",
            email = "juan@example.com",
            number = "0999999999",
            password = "plainPass"
        )

        whenever(userRepository.existsByEmail(request.email)).doReturn(true)

        assertThrows(IllegalArgumentException::class.java) {
            adminService.createUser(request)
        }
        verify(userRepository, never()).save(any())
    }

    // ── updateUser ──────────────────────────────────────────────────

    @Test
    fun `updateUser deberia actualizar datos preservando password y hotspots`() {
        val request = UserUpdateRequest(
            name = "Juan Actualizado",
            email = "juan.actualizado@example.com",
            number = "0977777777"
        )

        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as Users }

        val result = adminService.updateUser(1L, request)

        assertEquals(request.name, result.name)
        assertEquals(request.email, result.email)
        verify(userRepository).save(
            org.mockito.kotlin.argThat { user ->
                user.password == sampleUser.password && user.hotSpots == sampleUser.hotSpots
            }
        )
    }

    @Test
    fun `updateUser deberia lanzar excepcion si el usuario no existe`() {
        val request = UserUpdateRequest(
            name = "X",
            email = "x@example.com",
            number = "0900000000"
        )
        whenever(userRepository.findById(99L)).doReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            adminService.updateUser(99L, request)
        }
    }

    // ── resetPassword ───────────────────────────────────────────────

    @Test
    fun `resetPassword deberia codificar y guardar la nueva contrasena`() {
        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))
        whenever(passwordEncoder.encode("nuevaClave123")).doReturn("nuevaClaveEncoded")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as Users }

        adminService.resetPassword(1L, "nuevaClave123")

        verify(passwordEncoder).encode("nuevaClave123")
        verify(userRepository).save(
            org.mockito.kotlin.argThat { user -> user.password == "nuevaClaveEncoded" }
        )
    }

    @Test
    fun `resetPassword deberia lanzar excepcion si el usuario no existe`() {
        whenever(userRepository.findById(99L)).doReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            adminService.resetPassword(99L, "clave")
        }
        verify(passwordEncoder, never()).encode(any())
    }

    // ── deleteUser ──────────────────────────────────────────────────

    @Test
    fun `deleteUser deberia borrar location shares y luego el usuario`() {
        whenever(userRepository.existsById(1L)).doReturn(true)

        adminService.deleteUser(1L)

        verify(locationShareRepository, times(1)).deleteByUsersId(1L)
        verify(userRepository, times(1)).deleteById(1L)
    }

    @Test
    fun `deleteUser deberia lanzar excepcion si el usuario no existe`() {
        whenever(userRepository.existsById(99L)).doReturn(false)

        assertThrows(RuntimeException::class.java) {
            adminService.deleteUser(99L)
        }
        verify(locationShareRepository, never()).deleteByUsersId(any())
        verify(userRepository, never()).deleteById(any())
    }

    // ── getDashboardStats ───────────────────────────────────────────

    @Test
    fun `getDashboardStats deberia calcular correctamente las estadisticas`() {
        val hotspot1 = HotSpot(id = 1L, modality = "WIFI", active = true)
        val hotspot2 = HotSpot(id = 2L, modality = "WIFI", active = true)
        val hotspot3 = HotSpot(id = 3L, modality = "BLUETOOTH", active = true)

        whenever(userRepository.count()).doReturn(10L)
        whenever(hotSpotRepository.findByActiveTrue()).doReturn(listOf(hotspot1, hotspot2, hotspot3))
        whenever(locationShareRepository.countByActiveTrue()).doReturn(5L)

        val result = adminService.getDashboardStats()

        assertEquals(10, result.totalUsers)
        assertEquals(3, result.activeHotspotsTotal)
        assertEquals(2, result.hotspotsByModality["WIFI"])
        assertEquals(1, result.hotspotsByModality["BLUETOOTH"])
        assertEquals(5, result.activeShares)
    }
}