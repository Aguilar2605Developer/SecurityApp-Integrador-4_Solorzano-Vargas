package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.exceptions.HotSpotNotFoundException
import com.pucetec.securitydev.mappers.HotSpotMapper
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class HotSpotService(
    private val hotSpotRepository: HotSpotRepository,
    private val userRepository: UserRepository,
    private val hotSpotMapper: HotSpotMapper
) {

    // Crea un nuevo punto de peligro/alerta (botón de pánico o reporte manual)
    fun createHotSpot(request: HotSpotRequest): HotSpotResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Estudiante no encontrado con ID: ${request.userId}")
        }
        val entity = hotSpotMapper.toEntity(request, user)
        val savedEntity = hotSpotRepository.save(entity)
        return hotSpotMapper.toResponse(savedEntity)
    }

    // Devuelve todos los puntos de peligro registrados (para el mapa)
    fun getAllHotSpots(): List<HotSpotResponse> {
        return hotSpotRepository.findAll().map { hotSpotMapper.toResponse(it) }
    }

    // Busca un punto de peligro específico por su ID
    fun getHotSpotById(id: Long): HotSpotResponse {
        val hotSpot = hotSpotRepository.findById(id).orElseThrow {
            HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        return hotSpotMapper.toResponse(hotSpot)
    }

    // Actualiza un punto de peligro existente
    fun updateHotSpot(id: Long, request: HotSpotRequest): HotSpotResponse {
        if (!hotSpotRepository.existsById(id)) {
            throw HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        val user = userRepository.findById(request.userId).orElseThrow {
            RuntimeException("Estudiante no encontrado con ID: ${request.userId}")
        }
        val updatedEntity = hotSpotMapper.toEntity(request, user, id)
        val savedEntity = hotSpotRepository.save(updatedEntity)
        return hotSpotMapper.toResponse(savedEntity)
    }

    // Elimina un punto de peligro por su ID
    fun deleteHotSpot(id: Long) {
        if (!hotSpotRepository.existsById(id)) {
            throw HotSpotNotFoundException("Punto de peligro no encontrado con ID: $id")
        }
        hotSpotRepository.deleteById(id)
    }
}