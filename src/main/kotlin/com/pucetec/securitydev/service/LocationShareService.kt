package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.LocationShareRequest
import com.pucetec.securitydev.dto.LocationShareResponse
import com.pucetec.securitydev.entity.LocationShare
import com.pucetec.securitydev.exceptions.LocationShareNotFoundException
import com.pucetec.securitydev.mappers.LocationShareMapper
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class LocationShareService(
    private val locationShareRepository: LocationShareRepository,
    private val userRepository: UserRepository,
    private val locationShareMapper: LocationShareMapper
) {

    fun startSharing(request: LocationShareRequest): LocationShareResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: ${request.userId}")
        }
        val entity = locationShareMapper.toEntity(request, user)
        val saved = locationShareRepository.save(entity)
        return locationShareMapper.toResponse(saved)
    }

    fun updateLocation(shareId: String, latitude: Double, longitude: Double): LocationShareResponse {
        val existing = locationShareRepository.findByShareIdAndActiveTrue(shareId)
            ?: throw LocationShareNotFoundException("Compartir ubicación no encontrado o expirado: $shareId")

        val updated = LocationShare(
            id = existing.id,
            shareId = existing.shareId,
            latitude = latitude,
            longitude = longitude,
            active = true,
            expiresAt = existing.expiresAt,
            users = existing.users
        )
        return locationShareMapper.toResponse(locationShareRepository.save(updated))
    }

    fun getByShareId(shareId: String): LocationShareResponse {
        val share = getEntityByShareId(shareId)
        return locationShareMapper.toResponse(share)
    }

    // Devuelve la entidad completa (no el DTO) para poder vincular destinatarios,
    // comparar el dueño real, etc.
    fun getEntityByShareId(shareId: String): LocationShare {
        return locationShareRepository.findByShareId(shareId)
            ?: throw LocationShareNotFoundException("Compartir ubicación no encontrado: $shareId")
    }

    fun stopSharing(shareId: String): LocationShareResponse {
        val existing = locationShareRepository.findByShareIdAndActiveTrue(shareId)
            ?: throw LocationShareNotFoundException("Compartir ubicación no encontrado o expirado: $shareId")

        val stopped = LocationShare(
            id = existing.id,
            shareId = existing.shareId,
            latitude = existing.latitude,
            longitude = existing.longitude,
            active = false,
            expiresAt = existing.expiresAt,
            users = existing.users
        )
        return locationShareMapper.toResponse(locationShareRepository.save(stopped))
    }

    @Scheduled(fixedRate = 60000)
    fun deactivateExpiredShares() {
        val expired = locationShareRepository.findByActiveTrueAndExpiresAtBefore(LocalDateTime.now())
        expired.forEach {
            locationShareRepository.save(
                LocationShare(
                    id = it.id,
                    shareId = it.shareId,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    active = false,
                    expiresAt = it.expiresAt,
                    users = it.users
                )
            )
        }
    }
}