package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.LocationShareRequest
import com.pucetec.securitydev.dto.LocationShareResponse
import com.pucetec.securitydev.repository.LocationShareRecipientRepository
import com.pucetec.securitydev.service.EmailService
import com.pucetec.securitydev.service.LocationShareService
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
class LocationShareControllerTest {

    @Mock
    private lateinit var locationShareService: LocationShareService

    @Mock
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var locationShareRecipientRepository: LocationShareRecipientRepository

    @InjectMocks
    private lateinit var locationShareController: LocationShareController

    private lateinit var ownedResponse: LocationShareResponse
    private lateinit var sampleRequest: LocationShareRequest

    @BeforeEach
    fun setUp() {
        ownedResponse = LocationShareResponse(
            shareId = "share-123", latitude = -0.18, longitude = -78.46,
            username = "Juan", active = true,
            expiresAt = LocalDateTime.now().plusMinutes(10), userId = 1L
        )
        sampleRequest = LocationShareRequest(userId = 1L, latitude = 1.0, longitude = 1.0)
    }

    @AfterEach
    fun limpiarContexto() {
        SecurityContextHolder.clearContext()
    }

    private fun buildJwt(
        sub: String,
        email: String = "juan@example.com",
        emailVerified: Boolean = true
    ): Jwt {
        return Jwt.withTokenValue("token-de-prueba")
            .header("alg", "RS256")
            .claim("sub", sub)
            .claim("email", email)
            .claim("email_verified", emailVerified)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()
    }

    private fun autenticarComo(
        sub: String,
        localId: Long?,
        email: String = "juan@example.com",
        emailVerified: Boolean = true
    ) {
        val jwt = buildJwt(sub, email, emailVerified)
        val auth = UsernamePasswordAuthenticationToken(jwt, null, emptyList())
        SecurityContextHolder.getContext().authentication = auth
        whenever(userService.resolveLocalId(sub)).thenReturn(localId)
    }

    // ---------------------- updateLocation ----------------------

    @Test
    fun `updateLocation deberia permitir cuando el usuario es el dueño del share`() {
        autenticarComo("cognito-sub-1", 1L)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)
        whenever(locationShareService.updateLocation("share-123", 1.0, 1.0)).thenReturn(ownedResponse)

        val result = locationShareController.updateLocation("share-123", sampleRequest)

        assertEquals(200, result.statusCode.value())
        verify(locationShareService, times(1)).updateLocation("share-123", 1.0, 1.0)
    }

    @Test
    fun `updateLocation deberia lanzar AccessDenied cuando el usuario no es el dueño del share`() {
        autenticarComo("cognito-sub-2", 2L)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)

        assertThrows(AccessDeniedException::class.java) {
            locationShareController.updateLocation("share-123", sampleRequest)
        }
        verify(locationShareService, never()).updateLocation(any(), any(), any())
    }

    // ---------------------- stopSharing ----------------------

    @Test
    fun `stopSharing deberia lanzar AccessDenied cuando el usuario no es el dueño del share`() {
        autenticarComo("cognito-sub-2", 2L)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)

        assertThrows(AccessDeniedException::class.java) {
            locationShareController.stopSharing("share-123")
        }
        verify(locationShareService, never()).stopSharing(any())
    }

    @Test
    fun `stopSharing deberia permitir cuando el usuario es el dueño del share`() {
        autenticarComo("cognito-sub-1", 1L)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)
        whenever(locationShareService.stopSharing("share-123")).thenReturn(ownedResponse)

        val result = locationShareController.stopSharing("share-123")

        assertEquals(200, result.statusCode.value())
        verify(locationShareService, times(1)).stopSharing("share-123")
    }

    // ---------------------- getByShareId ----------------------

    @Test
    fun `getByShareId deberia permitir al dueño sin revisar destinatarios`() {
        autenticarComo("cognito-sub-1", 1L)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)

        val result = locationShareController.getByShareId("share-123")

        assertEquals(200, result.statusCode.value())
        verify(locationShareRecipientRepository, never())
            .existsByLocationShareShareIdAndEmail(any(), any())
    }

    @Test
    fun `getByShareId deberia permitir a un destinatario autorizado con correo verificado`() {
        autenticarComo("cognito-sub-2", 2L, email = "invitado@example.com", emailVerified = true)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)
        whenever(locationShareRecipientRepository.existsByLocationShareShareIdAndEmail("share-123", "invitado@example.com"))
            .thenReturn(true)

        val result = locationShareController.getByShareId("share-123")

        assertEquals(200, result.statusCode.value())
    }

    @Test
    fun `getByShareId deberia lanzar AccessDenied cuando el correo no esta autorizado`() {
        autenticarComo("cognito-sub-2", 2L, email = "intruso@example.com", emailVerified = true)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)
        whenever(locationShareRecipientRepository.existsByLocationShareShareIdAndEmail("share-123", "intruso@example.com"))
            .thenReturn(false)

        assertThrows(AccessDeniedException::class.java) {
            locationShareController.getByShareId("share-123")
        }
    }

    @Test
    fun `getByShareId deberia lanzar AccessDenied cuando el correo no esta verificado`() {
        autenticarComo("cognito-sub-2", 2L, email = "invitado@example.com", emailVerified = false)
        whenever(locationShareService.getByShareId("share-123")).thenReturn(ownedResponse)

        assertThrows(AccessDeniedException::class.java) {
            locationShareController.getByShareId("share-123")
        }
        verify(locationShareRecipientRepository, never())
            .existsByLocationShareShareIdAndEmail(any(), any())
    }
}