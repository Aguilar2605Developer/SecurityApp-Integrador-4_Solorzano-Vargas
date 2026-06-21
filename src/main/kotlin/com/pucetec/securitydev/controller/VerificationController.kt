package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.VerificationRequest
import com.pucetec.securitydev.dto.VerificationResponse
import com.pucetec.securitydev.service.VerificationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/verifications")
@CrossOrigin(origins = ["*"]) // 🌐 Habilita la comunicación con la app multiplataforma
class VerificationController(private val verificationService: VerificationService) {

    // Registra una nueva alerta o reporte de verificación enviado desde el celular
    @PostMapping
    fun createVerification(@RequestBody request: VerificationRequest): ResponseEntity<VerificationResponse> {
        val createdVerification = verificationService.createVerification(request)
        return ResponseEntity(createdVerification, HttpStatus.CREATED)
    }

    // Recupera todo el historial de alertas y verificaciones del sistema
    @GetMapping
    fun getAllVerifications(): ResponseEntity<List<VerificationResponse>> {
        return ResponseEntity.ok(verificationService.getAllVerifications())
    }

    // Busca un registro de alerta específico por su ID único
    @GetMapping("/{id}")
    fun getVerificationById(@PathVariable id: Long): ResponseEntity<VerificationResponse> {
        val verification = verificationService.getVerificationById(id)
        return if (verification != null) {
            ResponseEntity.ok(verification)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Actualiza una verificación existente
    @PutMapping("/{id}")
    fun updateVerification(@PathVariable id: Long, @RequestBody request: VerificationRequest): ResponseEntity<VerificationResponse> {
        val updatedVerification = verificationService.updateVerification(id, request)
        return if (updatedVerification != null) {
            ResponseEntity.ok(updatedVerification)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Elimina una verificación por su ID
    @DeleteMapping("/{id}")
    fun deleteVerification(@PathVariable id: Long): ResponseEntity<Void> {
        val deleted = verificationService.deleteVerification(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}