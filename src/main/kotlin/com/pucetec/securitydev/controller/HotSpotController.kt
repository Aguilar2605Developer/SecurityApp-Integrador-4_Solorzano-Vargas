package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.HotSpotRequest
import com.pucetec.securitydev.dto.HotSpotResponse
import com.pucetec.securitydev.security.CurrentUser
import com.pucetec.securitydev.service.HotSpotService
import com.pucetec.securitydev.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/hotspots")
@CrossOrigin(origins = ["*"])
class HotSpotController(
    private val hotSpotService: HotSpotService,
    private val userService: UserService
) {

    @PostMapping
    fun createHotSpot(@RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        val savedHotSpot = hotSpotService.createHotSpot(request)
        return ResponseEntity(savedHotSpot, HttpStatus.CREATED)
    }

    @GetMapping
    fun getAllHotSpots(): ResponseEntity<List<HotSpotResponse>> {
        return ResponseEntity.ok(hotSpotService.getAllHotSpots())
    }

    @GetMapping("/{id}")
    fun getHotSpotById(@PathVariable id: Long): ResponseEntity<HotSpotResponse> {
        return ResponseEntity.ok(hotSpotService.getHotSpotById(id))
    }

    @PutMapping("/{id}")
    fun updateHotSpot(@PathVariable id: Long, @RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        val existing = hotSpotService.getHotSpotById(id)
        val currentLocalId = userService.resolveLocalId(CurrentUser.sub())
        if (existing.userId != null && existing.userId != currentLocalId) {
            throw AccessDeniedException("No puedes editar un punto de peligro de otro usuario")
        }
        return ResponseEntity.ok(hotSpotService.updateHotSpot(id, request))
    }

    @PutMapping("/{id}/deactivate")
    fun deactivateHotSpot(@PathVariable id: Long): ResponseEntity<HotSpotResponse> {
        return ResponseEntity.ok(hotSpotService.deactivateHotSpot(id))
    }

    @DeleteMapping("/{id}")
    fun deleteHotSpot(@PathVariable id: Long): ResponseEntity<Void> {
        val existing = hotSpotService.getHotSpotById(id)
        val currentLocalId = userService.resolveLocalId(CurrentUser.sub())
        if (existing.userId != null && existing.userId != currentLocalId) {
            throw AccessDeniedException("No puedes eliminar un punto de peligro de otro usuario")
        }
        hotSpotService.deleteHotSpot(id)
        return ResponseEntity.noContent().build()
    }
}