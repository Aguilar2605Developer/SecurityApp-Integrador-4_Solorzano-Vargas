package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.VerificationRequest
import com.pucetec.securitydev.dto.VerificationResponse
import com.pucetec.securitydev.mappers.VerificationMapper
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.UserRepository
import com.pucetec.securitydev.repository.VerificationRepository
import org.springframework.stereotype.Service

@Service
class VerificationService(
    private val verificationRepository: VerificationRepository,
    private val userRepository: UserRepository,
    private val hotSpotRepository: HotSpotRepository,
    private val verificationMapper: VerificationMapper
) {

    // Registra una nueva verificación o alerta de seguridad enviada desde el campus
    fun createVerification(request: VerificationRequest): VerificationResponse {
        // 1. Buscamos primero el usuario real en la base de datos usando el ID del request
        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Estudiante no encontrado con ID: ${request.userId}")
        }

        // 2. Buscamos primero el punto caliente real en la base de datos usando el ID del request
        val hotSpot = hotSpotRepository.findById(request.hotSpotId).orElseThrow {
            RuntimeException("Punto de peligro no encontrado con ID: ${request.hotSpotId}")
        }

        // 3. Traduce los datos básicos PERO pasándole de una vez el 'user' y el 'hotSpot' encontrados
        // Nota: Si tu VerificationMapper.toEntity no acepta estos parámetros extras, mira el paso de abajo ⬇️
        val entity = verificationMapper.toEntity(request, user, hotSpot)

        // 4. Guarda el registro de verificación final en la base de datos de Neon Cloud
        val savedEntity = verificationRepository.save(entity)

        // 5. Retorna la respuesta formateada lista para ser consumida en el frontend
        return verificationMapper.toResponse(savedEntity)
    }

    // Recupera todo el historial de alertas y verificaciones del sistema
    fun getAllVerifications(): List<VerificationResponse> {
        return verificationRepository.findAll().map { verification ->
            verificationMapper.toResponse(verification)
        }
    }

    // Busca un registro de verificación específico usando su identificador único
    fun getVerificationById(id: Long): VerificationResponse? {
        val verification = verificationRepository.findById(id).orElse(null) ?: return null
        return verificationMapper.toResponse(verification)
    }

    // Actualiza una verificación existente
    fun updateVerification(id: Long, request: VerificationRequest): VerificationResponse? {
        val existing = verificationRepository.findById(id).orElse(null) ?: return null

        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Estudiante no encontrado con ID: ${request.userId}")
        }

        val hotSpot = hotSpotRepository.findById(request.hotSpotId).orElseThrow {
            RuntimeException("Punto de peligro no encontrado con ID: ${request.hotSpotId}")
        }

        val updatedEntity = verificationMapper.toEntity(request, user, hotSpot)
        val savedEntity = verificationRepository.save(updatedEntity)
        return verificationMapper.toResponse(savedEntity)
    }

    // Elimina una verificación por su ID
    fun deleteVerification(id: Long): Boolean {
        if (!verificationRepository.existsById(id)) return false
        verificationRepository.deleteById(id)
        return true
    }
}