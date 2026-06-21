package com.pucetec.securitydev.mappers

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.entity.Users
import org.springframework.stereotype.Component

@Component
class HotSpotMapper {

    fun toEntity(request: HotSpotRequest, users: Users?, id: Long = 0L): HotSpot {
        return HotSpot(
            id = id,
            latitude = request.latitude,
            longitude = request.longitude,
            modality = request.modality,
            description = request.description,
            users = users
        )
    }

    fun toResponse(hotSpot: HotSpot): HotSpotResponse {
        return HotSpotResponse(
            id = hotSpot.id,
            latitude = hotSpot.latitude,
            longitude = hotSpot.longitude,
            modality = hotSpot.modality,
            description = hotSpot.description,
            userId = hotSpot.users?.id,
            username = hotSpot.users?.name
        )
    }
}