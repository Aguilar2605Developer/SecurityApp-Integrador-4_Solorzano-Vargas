package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.LocationShareRequest
import com.pucetec.securitydev.dto.LocationShareResponse
import com.pucetec.securitydev.repository.LocationShareRecipientRepository
import com.pucetec.securitydev.security.CurrentUser
import com.pucetec.securitydev.service.EmailService
import com.pucetec.securitydev.service.LocationShareService
import com.pucetec.securitydev.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/location-shares")
class LocationShareController(
    private val locationShareService: LocationShareService,
    private val emailService: EmailService,
    private val userService: UserService,
    private val locationShareRecipientRepository: LocationShareRecipientRepository
) {

    data class ShareEmailRequest(val email: String)

    @PostMapping
    fun startSharing(@RequestBody request: LocationShareRequest): ResponseEntity<LocationShareResponse> {
        val response = locationShareService.startSharing(request)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{shareId}")
    fun updateLocation(
        @PathVariable shareId: String,
        @RequestBody request: LocationShareRequest
    ): ResponseEntity<LocationShareResponse> {
        val existing = locationShareService.getByShareId(shareId)
        requireOwner(existing.userId, "No puedes actualizar la ubicación de otro usuario")
        val response = locationShareService.updateLocation(shareId, request.latitude, request.longitude)
        return ResponseEntity.ok(response)
    }

    @PutMapping("/{shareId}/stop")
    fun stopSharing(@PathVariable shareId: String): ResponseEntity<LocationShareResponse> {
        val existing = locationShareService.getByShareId(shareId)
        requireOwner(existing.userId, "No puedes detener la ubicación de otro usuario")
        val response = locationShareService.stopSharing(shareId)
        return ResponseEntity.ok(response)
    }

    // GET ya NO es publico. Requiere JWT y que el correo del token (verificado
    // por Cognito) sea el dueño o un destinatario autorizado explicitamente.
    @GetMapping("/{shareId}")
    fun getByShareId(@PathVariable shareId: String): ResponseEntity<LocationShareResponse> {
        val response = locationShareService.getByShareId(shareId)

        val currentLocalId = userService.resolveLocalId(CurrentUser.sub())
        val isOwner = response.userId != null && response.userId == currentLocalId

        if (!isOwner) {
            if (!CurrentUser.emailVerified()) {
                throw AccessDeniedException("Tu correo no esta verificado. Confirma tu cuenta antes de continuar.")
            }
            val callerEmail = CurrentUser.email()?.trim()?.lowercase()
                ?: throw AccessDeniedException("No autorizado para ver esta ubicación")

            val isAuthorizedRecipient = locationShareRecipientRepository
                .existsByLocationShareShareIdAndEmail(shareId, callerEmail)

            if (!isAuthorizedRecipient) {
                throw AccessDeniedException("Esta ubicación no fue compartida con tu correo")
            }
        }

        return ResponseEntity.ok(response)
    }

    // Solo el dueño autoriza un nuevo destinatario y dispara el correo (sin token en el link).
    @PostMapping("/{shareId}/share-email")
    fun sendShareEmail(
        @PathVariable shareId: String,
        @RequestBody request: ShareEmailRequest
    ): ResponseEntity<Any> {
        val shareResponse = locationShareService.getByShareId(shareId)
        requireOwner(shareResponse.userId, "Solo el dueño puede compartir esta ubicación")

        val normalizedEmail = request.email.trim().lowercase()
        val shareEntity = locationShareService.getEntityByShareId(shareId)

        if (!locationShareRecipientRepository.existsByLocationShareShareIdAndEmail(shareId, normalizedEmail)) {
            locationShareRecipientRepository.save(
                com.pucetec.securitydev.entity.LocationShareRecipient(locationShare = shareEntity, email = normalizedEmail)
            )
        }

        emailService.sendLocationShareEmail(
            toEmail = normalizedEmail,
            username = shareResponse.username ?: "Usuario",
            shareId = shareId
        )

        return ResponseEntity.ok(mapOf("message" to "Correo enviado a $normalizedEmail"))
    }

    @GetMapping("/{shareId}/recipients")
    fun listRecipients(@PathVariable shareId: String): ResponseEntity<List<String>> {
        val shareResponse = locationShareService.getByShareId(shareId)
        requireOwner(shareResponse.userId, "Solo el dueño puede ver los destinatarios")
        val emails = locationShareRecipientRepository.findByLocationShareShareId(shareId).map { it.email }
        return ResponseEntity.ok(emails)
    }

    @DeleteMapping("/{shareId}/recipients/{email}")
    fun revokeRecipient(@PathVariable shareId: String, @PathVariable email: String): ResponseEntity<Void> {
        val shareResponse = locationShareService.getByShareId(shareId)
        requireOwner(shareResponse.userId, "Solo el dueño puede revocar destinatarios")
        locationShareRecipientRepository.deleteByLocationShareShareIdAndEmail(shareId, email.trim().lowercase())
        return ResponseEntity.noContent().build()
    }

    private fun requireOwner(shareOwnerId: Long?, message: String) {
        val currentLocalId = userService.resolveLocalId(CurrentUser.sub())
        if (shareOwnerId == null || shareOwnerId != currentLocalId) {
            throw AccessDeniedException(message)
        }
    }
}