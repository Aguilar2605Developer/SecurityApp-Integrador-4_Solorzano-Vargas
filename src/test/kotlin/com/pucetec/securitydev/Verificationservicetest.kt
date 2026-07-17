package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.VerificationRequest
import com.pucetec.securitydev.dto.VerificationResponse
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.entity.Verification
import com.pucetec.securitydev.mappers.VerificationMapper
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.UserRepository
import com.pucetec.securitydev.repository.VerificationRepository
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

@ExtendWith(MockitoExtension::class)
class VerificationServiceTest {

    @Mock
    private lateinit var verificationRepository: VerificationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var hotSpotRepository: HotSpotRepository

    @Mock
    private lateinit var verificationMapper: VerificationMapper

    @InjectMocks
    private lateinit var verificationService: VerificationService

    private lateinit var sampleUser: Users
    private lateinit var sampleHotSpot: HotSpot
    private lateinit var sampleRequest: VerificationRequest
    private lateinit var sampleEntity: Verification
    private lateinit var sampleResponse: VerificationResponse

    @BeforeEach
    fun setUp() {
        sampleUser = Users(
            id = 1L,
            cognitoSub = "cognito-sub-juan",
            name = "Juan Perez",
            email = "juanperez@example.com",
            number = "0999999999"
        )

        sampleHotSpot = HotSpot(
            id = 1L,
            latitude = -0.180653,
            longitude = -78.467838,
            modality = "Robo",
            description = "Zona peligrosa cerca de la entrada",
            peopleInvolved = 2,
            active = true,
            expiresAt = LocalDateTime.now().plusHours(24)
        )

        sampleRequest = VerificationRequest(
            userId = 1L,
            status = "CONFIRMADO",
            hotSpotId = 1L
        )

        sampleEntity = Verification(
            id = 1L,
            createdAt = LocalDateTime.now(),
            status = "CONFIRMADO",
            user = sampleUser,
            hotSpot = sampleHotSpot
        )

        sampleResponse = VerificationResponse(
            id = 1L,
            username = "Juan Perez",
            status = "CONFIRMADO",
            createdAt = sampleEntity.createdAt,
            userId = 1L,
            hotSpotId = 1L
        )
    }

    // ---------------------- createVerification ----------------------

    @Test
    fun `createVerification deberia crear y devolver la verificacion cuando usuario y hotspot existen`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser))
        whenever(hotSpotRepository.findById(1L)).thenReturn(Optional.of(sampleHotSpot))
        whenever(verificationMapper.toEntity(sampleRequest, sampleUser, sampleHotSpot)).thenReturn(sampleEntity)
        whenever(verificationRepository.save(sampleEntity)).thenReturn(sampleEntity)
        whenever(verificationMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = verificationService.createVerification(sampleRequest)

        assertEquals(sampleResponse.id, result.id)
        assertEquals(sampleResponse.status, result.status)
        verify(userRepository, times(1)).findById(1L)
        verify(hotSpotRepository, times(1)).findById(1L)
        verify(verificationRepository, times(1)).save(sampleEntity)
    }

    @Test
    fun `createVerification deberia lanzar excepcion cuando el usuario no existe`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows(RuntimeException::class.java) {
            verificationService.createVerification(sampleRequest)
        }

        assertTrue(exception.message!!.contains("Estudiante no encontrado"))
        verify(hotSpotRepository, never()).findById(any<Long>())
        verify(verificationRepository, never()).save(any<Verification>())
    }

    @Test
    fun `createVerification deberia lanzar excepcion cuando el hotspot no existe`() {
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser))
        whenever(hotSpotRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows(RuntimeException::class.java) {
            verificationService.createVerification(sampleRequest)
        }

        assertTrue(exception.message!!.contains("Punto de peligro no encontrado"))
        verify(verificationRepository, never()).save(any<Verification>())
    }

    // ---------------------- getAllVerifications ----------------------

    @Test
    fun `getAllVerifications deberia devolver la lista de verificaciones mapeadas`() {
        val secondUser = Users(
            id = 2L,
            cognitoSub = "cognito-sub-maria",
            name = "Maria Lopez",
            email = "marialopez@example.com",
            number = "0988888888"
        )
        val secondEntity = Verification(
            id = 2L,
            createdAt = LocalDateTime.now(),
            status = "PENDIENTE",
            user = secondUser,
            hotSpot = sampleHotSpot
        )
        val secondResponse = VerificationResponse(
            id = 2L,
            username = "Maria Lopez",
            status = "PENDIENTE",
            createdAt = secondEntity.createdAt,
            userId = 2L,
            hotSpotId = 1L
        )

        whenever(verificationRepository.findAll()).thenReturn(listOf(sampleEntity, secondEntity))
        whenever(verificationMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)
        whenever(verificationMapper.toResponse(secondEntity)).thenReturn(secondResponse)

        val result = verificationService.getAllVerifications()

        assertEquals(2, result.size)
        assertEquals(sampleResponse.status, result[0].status)
        assertEquals(secondResponse.status, result[1].status)
        verify(verificationRepository, times(1)).findAll()
    }

    @Test
    fun `getAllVerifications deberia devolver lista vacia cuando no hay verificaciones`() {
        whenever(verificationRepository.findAll()).thenReturn(emptyList())

        val result = verificationService.getAllVerifications()

        assertTrue(result.isEmpty())
        verify(verificationMapper, never()).toResponse(any<Verification>())
    }

    // ---------------------- getVerificationById ----------------------

    @Test
    fun `getVerificationById deberia devolver la verificacion cuando existe`() {
        whenever(verificationRepository.findById(1L)).thenReturn(Optional.of(sampleEntity))
        whenever(verificationMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = verificationService.getVerificationById(1L)

        assertNotNull(result)
        assertEquals(sampleResponse.id, result?.id)
        verify(verificationRepository, times(1)).findById(1L)
    }

    @Test
    fun `getVerificationById deberia devolver null cuando no existe`() {
        whenever(verificationRepository.findById(99L)).thenReturn(Optional.empty())

        val result = verificationService.getVerificationById(99L)

        assertNull(result)
        verify(verificationMapper, never()).toResponse(any<Verification>())
    }

    // ---------------------- updateVerification ----------------------

    @Test
    fun `updateVerification deberia actualizar y devolver la verificacion cuando todo existe`() {
        whenever(verificationRepository.findById(1L)).thenReturn(Optional.of(sampleEntity))
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser))
        whenever(hotSpotRepository.findById(1L)).thenReturn(Optional.of(sampleHotSpot))
        whenever(verificationMapper.toEntity(sampleRequest, sampleUser, sampleHotSpot, 1L)).thenReturn(sampleEntity)
        whenever(verificationRepository.save(sampleEntity)).thenReturn(sampleEntity)
        whenever(verificationMapper.toResponse(sampleEntity)).thenReturn(sampleResponse)

        val result = verificationService.updateVerification(1L, sampleRequest)

        assertNotNull(result)
        assertEquals(sampleResponse.id, result?.id)
        verify(verificationRepository, times(1)).save(sampleEntity)
    }

    @Test
    fun `updateVerification deberia devolver null cuando la verificacion no existe`() {
        whenever(verificationRepository.findById(99L)).thenReturn(Optional.empty())

        val result = verificationService.updateVerification(99L, sampleRequest)

        assertNull(result)
        verify(userRepository, never()).findById(any<Long>())
        verify(verificationRepository, never()).save(any<Verification>())
    }

    @Test
    fun `updateVerification deberia lanzar excepcion cuando el usuario no existe`() {
        whenever(verificationRepository.findById(1L)).thenReturn(Optional.of(sampleEntity))
        whenever(userRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows(RuntimeException::class.java) {
            verificationService.updateVerification(1L, sampleRequest)
        }

        assertTrue(exception.message!!.contains("Estudiante no encontrado"))
        verify(verificationRepository, never()).save(any<Verification>())
    }

    @Test
    fun `updateVerification deberia lanzar excepcion cuando el hotspot no existe`() {
        whenever(verificationRepository.findById(1L)).thenReturn(Optional.of(sampleEntity))
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser))
        whenever(hotSpotRepository.findById(1L)).thenReturn(Optional.empty())

        val exception = assertThrows(RuntimeException::class.java) {
            verificationService.updateVerification(1L, sampleRequest)
        }

        assertTrue(exception.message!!.contains("Punto de peligro no encontrado"))
        verify(verificationRepository, never()).save(any<Verification>())
    }

    // ---------------------- deleteVerification ----------------------

    @Test
    fun `deleteVerification deberia eliminar y devolver true cuando existe`() {
        whenever(verificationRepository.existsById(1L)).thenReturn(true)

        val result = verificationService.deleteVerification(1L)

        assertTrue(result)
        verify(verificationRepository, times(1)).deleteById(1L)
    }

    @Test
    fun `deleteVerification deberia devolver false cuando no existe`() {
        whenever(verificationRepository.existsById(99L)).thenReturn(false)

        val result = verificationService.deleteVerification(99L)

        assertFalse(result)
        verify(verificationRepository, never()).deleteById(any<Long>())
    }
}