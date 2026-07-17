package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.service.HotSpotService
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
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class HotSpotControllerTest {

    @Mock
    private lateinit var hotSpotService: HotSpotService

    @Mock
    private lateinit var userService: UserService

    @InjectMocks
    private lateinit var hotSpotController: HotSpotController

    private lateinit var sampleRequest: HotSpotRequest
    private lateinit var ownedResponse: HotSpotResponse
    private lateinit var anonymousResponse: HotSpotResponse

    @BeforeEach
    fun setUp() {
        sampleRequest = HotSpotRequest(
            latitude = -0.18, longitude = -78.46, modality = "robo",
            description = "test", userId = 1L, durationHours = 24, peopleInvolved = 1
        )
        ownedResponse = HotSpotResponse(
            id = 10L, latitude = -0.18, longitude = -78.46, modality = "robo",
            description = "test", userId = 1L, username = "Juan", active = true,
            expiresAt = LocalDateTime.now().plusHours(24), peopleInvolved = 1
        )
        anonymousResponse = HotSpotResponse(
            id = 11L, latitude = -0.18, longitude = -78.46, modality = "robo",
            description = "test", userId = null, username = null, active = true,
            expiresAt = LocalDateTime.now().plusHours(24), peopleInvolved = 1
        )
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

    private fun autenticarComo(sub: String, localId: Long?) {
        val jwt = buildJwt(sub)
        val auth = UsernamePasswordAuthenticationToken(jwt, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
        whenever(userService.resolveLocalId(sub)).thenReturn(localId)
    }

    // ---------------------- updateHotSpot ----------------------

    @Test
    fun `updateHotSpot deberia permitir la edicion cuando el usuario es el dueño`() {
        autenticarComo("cognito-sub-1", 1L)
        whenever(hotSpotService.getHotSpotById(10L)).thenReturn(ownedResponse)
        whenever(hotSpotService.updateHotSpot(10L, sampleRequest)).thenReturn(ownedResponse)

        val result = hotSpotController.updateHotSpot(10L, sampleRequest)

        assertEquals(200, result.statusCode.value())
        verify(hotSpotService, times(1)).updateHotSpot(10L, sampleRequest)
    }

    @Test
    fun `updateHotSpot deberia lanzar AccessDenied cuando el usuario no es el dueño`() {
        autenticarComo("cognito-sub-2", 2L)
        whenever(hotSpotService.getHotSpotById(10L)).thenReturn(ownedResponse)

        assertThrows(AccessDeniedException::class.java) {
            hotSpotController.updateHotSpot(10L, sampleRequest)
        }
        verify(hotSpotService, never()).updateHotSpot(any(), any())
    }

    @Test
    fun `updateHotSpot deberia permitir la edicion cuando el hotspot es anonimo (sin dueño)`() {
        autenticarComo("cognito-sub-2", 2L)
        whenever(hotSpotService.getHotSpotById(11L)).thenReturn(anonymousResponse)
        whenever(hotSpotService.updateHotSpot(11L, sampleRequest)).thenReturn(anonymousResponse)

        val result = hotSpotController.updateHotSpot(11L, sampleRequest)

        assertEquals(200, result.statusCode.value())
        verify(hotSpotService, times(1)).updateHotSpot(11L, sampleRequest)
    }

    // ---------------------- deleteHotSpot ----------------------

    @Test
    fun `deleteHotSpot deberia lanzar AccessDenied cuando el usuario no es el dueño`() {
        autenticarComo("cognito-sub-2", 2L)
        whenever(hotSpotService.getHotSpotById(10L)).thenReturn(ownedResponse)

        assertThrows(AccessDeniedException::class.java) {
            hotSpotController.deleteHotSpot(10L)
        }
        verify(hotSpotService, never()).deleteHotSpot(any())
    }

    @Test
    fun `deleteHotSpot deberia permitir eliminar cuando el usuario es el dueño`() {
        autenticarComo("cognito-sub-1", 1L)
        whenever(hotSpotService.getHotSpotById(10L)).thenReturn(ownedResponse)
        doNothing().whenever(hotSpotService).deleteHotSpot(10L)

        val result = hotSpotController.deleteHotSpot(10L)

        assertEquals(204, result.statusCode.value())
        verify(hotSpotService, times(1)).deleteHotSpot(10L)
    }
}