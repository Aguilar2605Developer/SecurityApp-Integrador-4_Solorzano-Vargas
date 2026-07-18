package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserCreateRequest
import com.pucetec.securitydev.dto.UserUpdateRequest
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.entity.HotSpotReport
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.HotSpotReportRepository
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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    lateinit var userRepository: UserRepository

    @Mock
    lateinit var hotSpotRepository: HotSpotRepository

    @Mock
    lateinit var hotSpotReportRepository: HotSpotReportRepository

    @Mock
    lateinit var locationShareRepository: LocationShareRepository

    @Mock
    lateinit var cognitoAdminService: CognitoAdminService

    private lateinit var adminService: AdminService

    private lateinit var sampleUser: Users

    @BeforeEach
    fun setUp() {
        adminService = AdminService(
            userRepository,
            hotSpotRepository,
            hotSpotReportRepository,
            locationShareRepository,
            cognitoAdminService
        )

        sampleUser = Users(
            id = 1L,
            cognitoSub = "cognito-sub-abc",
            name = "Juan Perez",
            email = "juan@example.com",
            number = "0999999999",
            hotSpotReports = mutableListOf()
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
    fun `createUser deberia crear el usuario en Cognito y guardar el perfil local cuando el correo no existe`() {
        val request = UserCreateRequest(
            name = "Nuevo Usuario",
            email = "nuevo@example.com",
            number = "0988888888",
            password = "plainPass"
        )

        whenever(userRepository.existsByEmail(request.email)).doReturn(false)
        whenever(cognitoAdminService.createUser(request.email, request.name, request.password))
            .doReturn("cognito-sub-nuevo")
        whenever(userRepository.save(any())).thenAnswer { it.arguments[0] as Users }

        val result = adminService.createUser(request)

        assertEquals(request.name, result.name)
        assertEquals(request.email, result.email)
        verify(cognitoAdminService, times(1)).createUser(request.email, request.name, request.password)
        verify(cognitoAdminService, times(1)).addUserToGroup(request.email, "USER")
        verify(userRepository, times(1)).save(
            org.mockito.kotlin.argThat { user -> user.cognitoSub == "cognito-sub-nuevo" }
        )
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
        verify(cognitoAdminService, never()).createUser(any(), any(), any())
        verify(userRepository, never()).save(any())
    }

    // ── updateUser ──────────────────────────────────────────────────

    @Test
    fun `updateUser deberia actualizar datos preservando cognitoSub y hotSpotReports`() {
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
                user.cognitoSub == sampleUser.cognitoSub && user.hotSpotReports == sampleUser.hotSpotReports
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
    fun `resetPassword deberia delegar en Cognito usando el email del usuario`() {
        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))

        adminService.resetPassword(1L, "nuevaClave123")

        verify(cognitoAdminService, times(1)).resetPassword(sampleUser.email, "nuevaClave123")
        verify(userRepository, never()).save(any())
    }

    @Test
    fun `resetPassword deberia lanzar excepcion si el usuario no existe`() {
        whenever(userRepository.findById(99L)).doReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            adminService.resetPassword(99L, "clave")
        }
        verify(cognitoAdminService, never()).resetPassword(any(), any())
    }

    // ── deleteUser ──────────────────────────────────────────────────

    @Test
    fun `deleteUser deberia borrar location shares, borrar en Cognito y luego el usuario`() {
        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))

        adminService.deleteUser(1L)

        verify(locationShareRepository, times(1)).deleteByUsersId(1L)
        verify(cognitoAdminService, times(1)).deleteUser(sampleUser.email)
        verify(userRepository, times(1)).deleteById(1L)
    }

    @Test
    fun `deleteUser deberia lanzar excepcion si el usuario no existe`() {
        whenever(userRepository.findById(99L)).doReturn(Optional.empty())

        assertThrows(RuntimeException::class.java) {
            adminService.deleteUser(99L)
        }
        verify(locationShareRepository, never()).deleteByUsersId(any())
        verify(cognitoAdminService, never()).deleteUser(any())
        verify(userRepository, never()).deleteById(any())
    }

    // ── getDashboardStats ───────────────────────────────────────────

    @Test
    fun `getDashboardStats deberia calcular correctamente las estadisticas`() {
        val hotspot1 = HotSpot(id = 1L, active = true)
        val hotspot2 = HotSpot(id = 2L, active = true)
        val hotspot3 = HotSpot(id = 3L, active = true)

        val report1 = HotSpotReport(id = 100L, modality = "WIFI", hotSpot = hotspot1)
        val report2 = HotSpotReport(id = 101L, modality = "WIFI", hotSpot = hotspot2)
        val report3 = HotSpotReport(id = 102L, modality = "BLUETOOTH", hotSpot = hotspot3)

        whenever(userRepository.count()).doReturn(10L)
        whenever(hotSpotRepository.findByActiveTrue()).doReturn(listOf(hotspot1, hotspot2, hotspot3))
        whenever(hotSpotReportRepository.findByHotSpotIdIn(listOf(1L, 2L, 3L)))
            .doReturn(listOf(report1, report2, report3))
        whenever(locationShareRepository.countByActiveTrue()).doReturn(5L)

        val result = adminService.getDashboardStats()

        assertEquals(10, result.totalUsers)
        assertEquals(3, result.activeHotspotsTotal)
        assertEquals(2, result.hotspotsByModality["WIFI"])
        assertEquals(1, result.hotspotsByModality["BLUETOOTH"])
        assertEquals(5, result.activeShares)
    }
}