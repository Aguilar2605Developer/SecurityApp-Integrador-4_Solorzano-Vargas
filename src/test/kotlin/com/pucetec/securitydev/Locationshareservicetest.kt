package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.LocationShareRequest
import com.pucetec.securitydev.dto.LocationShareResponse
import com.pucetec.securitydev.entity.LocationShare
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.exceptions.LocationShareNotFoundException
import com.pucetec.securitydev.mappers.LocationShareMapper
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

/**
 * Todas las clases usadas en este test han sido CONFIRMADAS contra el código real del proyecto:
 *  - entity.LocationShare: id: Long, shareId: String, latitude: Double, longitude: Double,
 *                          active: Boolean, expiresAt: LocalDateTime, users: Users? (@ManyToOne)
 *  - entity.Users: id: Long, name: String, email: String, number: String, password: String,
 *                  hotSpots: MutableList<HotSpot>
 *  - mappers.LocationShareMapper: toEntity(request, users: Users?): LocationShare
 *                                 toResponse(entity): LocationShareResponse (username = entity.users?.name)
 *  - repository.LocationShareRepository: findByShareId, findByShareIdAndActiveTrue,
 *                                        findByActiveTrueAndExpiresAtBefore, save
 *  - repository.UserRepository: JpaRepository<Users, Long> estándar (findById)
 *  - exceptions.LocationShareNotFoundException(message: String) : RuntimeException(message)
 */
@ExtendWith(MockitoExtension::class)
class LocationShareServiceTest {

    @Mock
    private lateinit var locationShareRepository: LocationShareRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var locationShareMapper: LocationShareMapper

    @InjectMocks
    private lateinit var locationShareService: LocationShareService

    private lateinit var sampleUser: Users
    private lateinit var sampleRequest: LocationShareRequest
    private lateinit var sampleEntity: LocationShare
    private lateinit var sampleResponse: LocationShareResponse

    @BeforeEach
    fun setUp() {
        sampleUser = Users(id = 1L, name = "Juan Perez", email = "juanperez@example.com", number = "0999999999", password = "hashed-password")

        sampleRequest = LocationShareRequest(
            userId = 1L,
            latitude = -0.180653,
            longitude = -78.467838
        )

        sampleEntity = LocationShare(
            id = 1L,
            shareId = "share-123",
            latitude = -0.180653,
            longitude = -78.467838,
            active = true,
            expiresAt = LocalDateTime.now().plusHours(1),
            users = sampleUser
        )

        sampleResponse = LocationShareResponse(
            shareId = "share-123",
            latitude = -0.180653,
            longitude = -78.467838,
            username = "Juan Perez",
            active = true,
            expiresAt = sampleEntity.expiresAt
        )
    }

    // ---------------------- startSharing ----------------------

    @Test
    fun `startSharing deberia crear y devolver el location share cuando el usuario existe`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser))
        whenever(locationShareMapper.toEntity(sampleRequest, sampleUser)).thenReturn(sampleEntity)
        whenever(locationShareRepository.save(sampleEntity)).thenReturn(sampleEntity)
        whenever(locationShareMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = locationShareService.startSharing(sampleRequest)

        assertEquals(sampleResponse.shareId, result.shareId)
        assertEquals(sampleResponse.username, result.username)
        assertTrue(result.active)
        verify(userRepository, times(1)).findById(1L)
        verify(locationShareRepository, times(1)).save(sampleEntity)
    }

    @Test
    fun `startSharing deberia lanzar excepcion cuando el usuario no existe`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows(RuntimeException::class.java) {
            locationShareService.startSharing(sampleRequest)
        }

        assertTrue(exception.message!!.contains("Usuario no encontrado"))
        verify(locationShareRepository, never()).save(any<LocationShare>())
    }

    // ---------------------- updateLocation ----------------------

    @Test
    fun `updateLocation deberia actualizar la ubicacion cuando el share existe y esta activo`() {
        val newLat = 10.0
        val newLng = 20.0

        whenever(locationShareRepository.findByShareIdAndActiveTrue("share-123")).thenReturn(sampleEntity)
        whenever(locationShareRepository.save(any<LocationShare>())).thenReturn(sampleEntity)
        whenever(locationShareMapper.toResponse(any<LocationShare>())).thenReturn(sampleResponse)

        val result = locationShareService.updateLocation("share-123", newLat, newLng)

        assertNotNull(result)
        verify(locationShareRepository, times(1)).findByShareIdAndActiveTrue("share-123")
        verify(locationShareRepository, times(1)).save(any<LocationShare>())
    }

    @Test
    fun `updateLocation deberia lanzar excepcion cuando el share no existe o esta expirado`() {
        whenever(locationShareRepository.findByShareIdAndActiveTrue("share-inexistente")).thenReturn(null)

        val exception = assertThrows(LocationShareNotFoundException::class.java) {
            locationShareService.updateLocation("share-inexistente", 1.0, 1.0)
        }

        assertTrue(exception.message!!.contains("share-inexistente"))
        verify(locationShareRepository, never()).save(any<LocationShare>())
    }

    // ---------------------- getByShareId ----------------------

    @Test
    fun `getByShareId deberia devolver el share cuando existe`() {
        whenever(locationShareRepository.findByShareId("share-123")).thenReturn(sampleEntity)
        whenever(locationShareMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = locationShareService.getByShareId("share-123")

        assertEquals(sampleResponse.shareId, result.shareId)
        verify(locationShareRepository, times(1)).findByShareId("share-123")
    }

    @Test
    fun `getByShareId deberia lanzar excepcion cuando el share no existe`() {
        whenever(locationShareRepository.findByShareId("share-inexistente")).thenReturn(null)

        val exception = assertThrows(LocationShareNotFoundException::class.java) {
            locationShareService.getByShareId("share-inexistente")
        }

        assertTrue(exception.message!!.contains("share-inexistente"))
    }

    // ---------------------- stopSharing ----------------------

    @Test
    fun `stopSharing deberia desactivar el share cuando existe y esta activo`() {
        whenever(locationShareRepository.findByShareIdAndActiveTrue("share-123")).thenReturn(sampleEntity)
        whenever(locationShareRepository.save(any<LocationShare>())).thenAnswer { invocation ->
            invocation.getArgument<LocationShare>(0)
        }
        whenever(locationShareMapper.toResponse(any<LocationShare>())).thenReturn(
            LocationShareResponse(
                shareId = sampleResponse.shareId,
                latitude = sampleResponse.latitude,
                longitude = sampleResponse.longitude,
                username = sampleResponse.username,
                active = false,
                expiresAt = sampleResponse.expiresAt
            )
        )

        val result = locationShareService.stopSharing("share-123")

        assertFalse(result.active)
        verify(locationShareRepository, times(1)).findByShareIdAndActiveTrue("share-123")
        verify(locationShareRepository, times(1)).save(any<LocationShare>())
    }

    @Test
    fun `stopSharing deberia lanzar excepcion cuando el share no existe o esta expirado`() {
        whenever(locationShareRepository.findByShareIdAndActiveTrue("share-inexistente")).thenReturn(null)

        assertThrows(LocationShareNotFoundException::class.java) {
            locationShareService.stopSharing("share-inexistente")
        }

        verify(locationShareRepository, never()).save(any<LocationShare>())
    }

    // ---------------------- deactivateExpiredShares ----------------------

    @Test
    fun `deactivateExpiredShares deberia desactivar todos los shares expirados`() {
        val expiredShare1 = LocationShare(
            id = 2L,
            shareId = "share-expired-1",
            latitude = 1.0,
            longitude = 1.0,
            active = true,
            expiresAt = LocalDateTime.now().minusHours(1),
            users = sampleUser
        )
        val expiredShare2 = LocationShare(
            id = 3L,
            shareId = "share-expired-2",
            latitude = 2.0,
            longitude = 2.0,
            active = true,
            expiresAt = LocalDateTime.now().minusMinutes(30),
            users = sampleUser
        )

        whenever(locationShareRepository.findByActiveTrueAndExpiresAtBefore(any<LocalDateTime>()))
            .thenReturn(listOf(expiredShare1, expiredShare2))
        whenever(locationShareRepository.save(any<LocationShare>())).thenAnswer { invocation ->
            invocation.getArgument<LocationShare>(0)
        }

        locationShareService.deactivateExpiredShares()

        verify(locationShareRepository, times(1))
            .findByActiveTrueAndExpiresAtBefore(any<LocalDateTime>())
        verify(locationShareRepository, times(2)).save(any<LocationShare>())
    }

    @Test
    fun `deactivateExpiredShares no deberia guardar nada cuando no hay shares expirados`() {
        whenever(locationShareRepository.findByActiveTrueAndExpiresAtBefore(any<LocalDateTime>()))
            .thenReturn(emptyList())

        locationShareService.deactivateExpiredShares()

        verify(locationShareRepository, never()).save(any<LocationShare>())
    }
}