package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.service.HotSpotService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/hotspots")
@CrossOrigin(origins = ["*"]) // 🌐 Evita bloqueos de CORS al conectar con Ionic
class HotSpotController(private val hotSpotService: HotSpotService) {

    // Guarda un nuevo punto de peligro detectado en el campus
    @PostMapping
    fun createHotSpot(@RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        val savedHotSpot = hotSpotService.createHotSpot(request)
        return ResponseEntity(savedHotSpot, HttpStatus.CREATED)
    }

    // Devuelve la lista de todos los puntos calientes para cargarlos en el mapa
    @GetMapping
    fun getAllHotSpots(): ResponseEntity<List<HotSpotResponse>> {
        return ResponseEntity.ok(hotSpotService.getAllHotSpots())
    }

    // Busca un punto caliente específico mediante su ID único
    @GetMapping("/{id}")
    fun getHotSpotById(@PathVariable id: Long): ResponseEntity<HotSpotResponse> {
        return ResponseEntity.ok(hotSpotService.getHotSpotById(id))
    }

    // Actualiza un punto caliente existente
    @PutMapping("/{id}")
    fun updateHotSpot(@PathVariable id: Long, @RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        return ResponseEntity.ok(hotSpotService.updateHotSpot(id, request))
    }

    // Elimina un punto caliente por su ID
    @DeleteMapping("/{id}")
    fun deleteHotSpot(@PathVariable id: Long): ResponseEntity<Void> {
        hotSpotService.deleteHotSpot(id)
        return ResponseEntity.noContent().build()
    }
}