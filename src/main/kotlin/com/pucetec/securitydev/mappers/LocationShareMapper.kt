package com.pucetec.securitydev.mappers

import com.pucetec.securitydev.dto.LocationShareRequest
import com.pucetec.securitydev.dto.LocationShareResponse
import com.pucetec.securitydev.entity.LocationShare
import com.pucetec.securitydev.entity.Users
import org.springframework.stereotype.Component

@Component
class LocationShareMapper {

    fun toEntity(request: LocationShareRequest, users: Users?): LocationShare {
        return LocationShare(
            latitude = request.latitude,
            longitude = request.longitude,
            users = users
        )
    }

    fun toResponse(locationShare: LocationShare): LocationShareResponse {
        return LocationShareResponse(
            shareId = locationShare.shareId,
            latitude = locationShare.latitude,
            longitude = locationShare.longitude,
            username = locationShare.users?.name,
            active = locationShare.active,
            expiresAt = locationShare.expiresAt,
            userId = locationShare.users?.id
        )
    }
}