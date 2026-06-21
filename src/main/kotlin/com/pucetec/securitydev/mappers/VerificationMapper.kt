package com.pucetec.securitydev.mappers

import com.pucetec.securitydev.dto.VerificationRequest
import com.pucetec.securitydev.dto.VerificationResponse
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.entity.Verification
import com.pucetec.securitydev.entity.HotSpot
import org.springframework.stereotype.Component

@Component
class VerificationMapper {

    // 1. Traduce de la App Móvil hacia la Base de Datos (Neon Cloud)
    fun toEntity(
        request: VerificationRequest,
        user: Users,
        hotSpot: HotSpot,
        id: Long = 0L
    ): Verification {
        return Verification(
            id = id,
            hotSpot = hotSpot,
            status = request.status,
            user = user
        )
    }

    // 2. Traduce de la Base de Datos hacia la App Móvil (Ionic)
    fun toResponse(verification: Verification): VerificationResponse {
        return VerificationResponse(
            id = verification.id,
            userId = verification.user.id,
            username = verification.user.name,
            hotSpotId = verification.hotSpot.id,
            status = verification.status,
            createdAt = verification.createdAt
        )
    }
}