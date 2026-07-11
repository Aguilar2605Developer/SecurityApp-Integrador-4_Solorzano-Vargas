package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.entity.HotSpot
import com.pucetec.securitydev.exceptions.HotSpotNotFoundException
import com.pucetec.securitydev.mappers.HotSpotMapper
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class HotSpotService(
    private val hotSpotRepository: HotSpotRepository,
    private val userRepository: UserRepository,
    private val hotSpotMapper: HotSpotMapper
) {

    fun createHotSpot(request: HotSpotRequest): HotSpotResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: ${request.userId}")
        }
        val entity = hotSpotMapper.toEntity(request, user)
        val savedEntity = hotSpotRepository.save(entity)
        return hotSpotMapper.toResponse(savedEntity)
    }

    fun getAllHotSpots(): List<HotSpotResponse> {
        return hotSpotRepository.findByActiveTrue().map { hotSpotMapper.toResponse(it) }
    }

    // Admin ve TODO, incluidas las inactivas/expiradas
    fun getAllHotSpotsAdmin(): List<HotSpotResponse> {
        return hotSpotRepository.findAll().map { hotSpotMapper.toResponse(it) }
    }

    fun getHotSpotById(id: Long): HotSpotResponse {
        val hotSpot = hotSpotRepository.findById(id).orElseThrow {
            HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        return hotSpotMapper.toResponse(hotSpot)
    }

    fun updateHotSpot(id: Long, request: HotSpotRequest): HotSpotResponse {
        if (!hotSpotRepository.existsById(id)) {
            throw HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: ${request.userId}")
        }
        val updatedEntity = hotSpotMapper.toEntity(request, user, id)
        val savedEntity = hotSpotRepository.save(updatedEntity)
        return hotSpotMapper.toResponse(savedEntity)
    }

    fun deactivateHotSpot(id: Long): HotSpotResponse {
        val hotSpot = hotSpotRepository.findById(id).orElseThrow {
            HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        val deactivated = HotSpot(
            id = hotSpot.id,
            latitude = hotSpot.latitude,
            longitude = hotSpot.longitude,
            modality = hotSpot.modality,
            description = hotSpot.description,
            active = false,
            expiresAt = hotSpot.expiresAt,
            users = hotSpot.users
        )
        return hotSpotMapper.toResponse(hotSpotRepository.save(deactivated))
    }

    fun deleteHotSpot(id: Long) {
        if (!hotSpotRepository.existsById(id)) {
            throw HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        hotSpotRepository.deleteById(id)
    }

    @Scheduled(fixedRate = 60000)
    fun deactivateExpiredHotSpots() {
        val expired = hotSpotRepository.findByActiveTrueAndExpiresAtBefore(LocalDateTime.now())
        expired.forEach {
            hotSpotRepository.save(
                HotSpot(
                    id = it.id,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    modality = it.modality,
                    description = it.description,
                    active = false,
                    expiresAt = it.expiresAt,
                    users = it.users
                )
            )
        }
    }
}