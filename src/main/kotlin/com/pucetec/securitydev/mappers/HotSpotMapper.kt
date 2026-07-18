package com.pucetec.securitydev.mappers

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.entity.HotSpotReport
import com.pucetec.securitydev.entity.Users
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class HotSpotMapper {

    // Construye solo la parte "geográfica" del punto de peligro
    fun toHotSpotEntity(request: HotSpotRequest, id: Long = 0L): HotSpot {
        return HotSpot(
            id = id,
            latitude = request.latitude,
            longitude = request.longitude,
            active = true,
            expiresAt = LocalDateTime.now().plusHours(request.durationHours)
        )
    }

    // Construye el reporte asociado (modalidad, descripción, personas involucradas, quién reporta)
    fun toReportEntity(request: HotSpotRequest, hotSpot: HotSpot, users: Users?, id: Long = 0L): HotSpotReport {
        return HotSpotReport(
            id = id,
            modality = request.modality,
            description = request.description,
            peopleInvolved = request.peopleInvolved,
            hotSpot = hotSpot,
            users = users
        )
    }

    // Combina HotSpot + su reporte en la misma respuesta plana que ya consume el frontend
    fun toResponse(hotSpot: HotSpot, report: HotSpotReport?): HotSpotResponse {
        return HotSpotResponse(
            id = hotSpot.id,
            latitude = hotSpot.latitude,
            longitude = hotSpot.longitude,
            modality = report?.modality ?: "",
            description = report?.description ?: "",
            userId = report?.users?.id,
            username = report?.users?.name,
            active = hotSpot.active,
            expiresAt = hotSpot.expiresAt,
            peopleInvolved = report?.peopleInvolved ?: 0
        )
    }
}