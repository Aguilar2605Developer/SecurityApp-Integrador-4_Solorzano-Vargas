package com.pucetec.securitydev

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.exceptions.HotSpotNotFoundException
import com.pucetec.securitydev.mappers.HotSpotMapper
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.UserRepository
import com.pucetec.securitydev.service.HotSpotService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class HotSpotServiceTest {

    @Mock lateinit var hotSpotRepository: HotSpotRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var hotSpotMapper: HotSpotMapper

    private lateinit var hotSpotService: HotSpotService
    private lateinit var sampleUser: Users
    private lateinit var sampleHotSpot: HotSpot
    private lateinit var sampleRequest: HotSpotRequest
    private lateinit var sampleResponse: HotSpotResponse

    @BeforeEach
    fun setUp() {
        hotSpotService = HotSpotService(hotSpotRepository, userRepository, hotSpotMapper)

        sampleUser = Users(id = 1L, name = "Juan", email = "j@e.com", number = "123", password = "p")

        sampleHotSpot = HotSpot(
            id = 10L, latitude = -0.1, longitude = -78.4, modality = "ROBO",
            description = "Test", peopleInvolved = 2, active = true,
            expiresAt = LocalDateTime.now().plusHours(2), users = sampleUser
        )

        sampleRequest = HotSpotRequest(
            latitude = -0.1, longitude = -78.4, modality = "ROBO",
            description = "Test", userId = 1L, durationHours = 24, peopleInvolved = 2
        )

        sampleResponse = HotSpotResponse(
            id = 10L, latitude = -0.1, longitude = -78.4, modality = "ROBO",
            description = "Test", userId = 1L, username = "Juan", active = true,
            expiresAt = sampleHotSpot.expiresAt, peopleInvolved = 2
        )
    }

    @Test
    fun `createHotSpot deberia crear y retornar el hotspot`() {
        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))
        whenever(hotSpotMapper.toEntity(any<HotSpotRequest>(), any<Users>(), any())).doReturn(sampleHotSpot)
        whenever(hotSpotRepository.save(any())).doReturn(sampleHotSpot)
        whenever(hotSpotMapper.toResponse(any())).doReturn(sampleResponse)

        val result = hotSpotService.createHotSpot(sampleRequest)
        assertEquals(10L, result.id)
        verify(hotSpotRepository).save(any())
    }

    @Test
    fun `createHotSpot deberia lanzar excepcion si el usuario no existe`() {
        whenever(userRepository.findById(sampleRequest.userId)).doReturn(Optional.empty())
        assertThrows(RuntimeException::class.java) { hotSpotService.createHotSpot(sampleRequest) }
    }

    @Test
    fun `getAllHotSpots deberia retornar solo activos`() {
        whenever(hotSpotRepository.findByActiveTrue()).doReturn(listOf(sampleHotSpot))
        whenever(hotSpotMapper.toResponse(any())).doReturn(sampleResponse)

        val result = hotSpotService.getAllHotSpots()
        assertEquals(1, result.size)
        verify(hotSpotRepository).findByActiveTrue()
    }

    @Test
    fun `getAllHotSpotsAdmin deberia retornar todos`() {
        whenever(hotSpotRepository.findAll()).doReturn(listOf(sampleHotSpot))
        whenever(hotSpotMapper.toResponse(any())).doReturn(sampleResponse)

        val result = hotSpotService.getAllHotSpotsAdmin()
        assertEquals(1, result.size)
        verify(hotSpotRepository).findAll()
    }

    @Test
    fun `getHotSpotById deberia retornar si existe`() {
        whenever(hotSpotRepository.findById(10L)).doReturn(Optional.of(sampleHotSpot))
        whenever(hotSpotMapper.toResponse(any())).doReturn(sampleResponse)

        val result = hotSpotService.getHotSpotById(10L)
        assertEquals(10L, result.id)
    }

    @Test
    fun `getHotSpotById deberia lanzar excepcion si no existe`() {
        whenever(hotSpotRepository.findById(99L)).doReturn(Optional.empty())
        assertThrows(HotSpotNotFoundException::class.java) { hotSpotService.getHotSpotById(99L) }
    }

    @Test
    fun `updateHotSpot deberia actualizar y retornar`() {
        whenever(hotSpotRepository.existsById(10L)).doReturn(true)
        whenever(userRepository.findById(1L)).doReturn(Optional.of(sampleUser))
        // El service real llama: hotSpotMapper.toEntity(request, user, id)
        whenever(hotSpotMapper.toEntity(any<HotSpotRequest>(), any<Users>(), any<Long>())).doReturn(sampleHotSpot)
        whenever(hotSpotRepository.save(any())).doReturn(sampleHotSpot)
        whenever(hotSpotMapper.toResponse(any())).doReturn(sampleResponse)

        val result = hotSpotService.updateHotSpot(10L, sampleRequest)
        assertEquals(10L, result.id)
        verify(hotSpotRepository).save(any())
    }

    @Test
    fun `updateHotSpot deberia lanzar excepcion si no existe`() {
        whenever(hotSpotRepository.existsById(99L)).doReturn(false)
        assertThrows(HotSpotNotFoundException::class.java) { hotSpotService.updateHotSpot(99L, sampleRequest) }
    }

    @Test
    fun `deactivateHotSpot deberia guardar con active false`() {
        whenever(hotSpotRepository.findById(10L)).doReturn(Optional.of(sampleHotSpot))
        whenever(hotSpotRepository.save(any())).thenAnswer { it.arguments[0] as HotSpot }
        whenever(hotSpotMapper.toResponse(any())).doReturn(sampleResponse.copy(active = false))

        val result = hotSpotService.deactivateHotSpot(10L)
        val captor = argumentCaptor<HotSpot>()
        verify(hotSpotRepository).save(captor.capture())
        assertFalse(captor.firstValue.active)
        assertFalse(result.active)
    }

    @Test
    fun `deleteHotSpot deberia eliminar si existe`() {
        whenever(hotSpotRepository.existsById(10L)).doReturn(true)
        hotSpotService.deleteHotSpot(10L)
        verify(hotSpotRepository).deleteById(10L)
    }

    @Test
    fun `deleteHotSpot deberia lanzar excepcion si no existe`() {
        whenever(hotSpotRepository.existsById(99L)).doReturn(false)
        assertThrows(HotSpotNotFoundException::class.java) { hotSpotService.deleteHotSpot(99L) }
    }

    @Test
    fun `deactivateExpiredHotSpots deberia desactivar expirados`() {
        val expirado = sampleHotSpot.copy(expiresAt = LocalDateTime.now().minusMinutes(1))
        whenever(hotSpotRepository.findByActiveTrueAndExpiresAtBefore(any())).doReturn(listOf(expirado))
        whenever(hotSpotRepository.save(any())).thenAnswer { it.arguments[0] }

        hotSpotService.deactivateExpiredHotSpots()
        val captor = argumentCaptor<HotSpot>()
        verify(hotSpotRepository).save(captor.capture())
        assertFalse(captor.firstValue.active)
    }
}