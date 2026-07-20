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

    // El userId del reporte SIEMPRE se toma del JWT, nunca del body: antes
    // request.userId venia tal cual del cliente, asi que cualquiera podia
    // crear un reporte "a nombre" de otro usuario con solo cambiar ese campo.
    @PostMapping
    fun createHotSpot(@RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        val currentLocalId = requireCurrentLocalId()
        val safeRequest = request.copy(userId = currentLocalId)
        val savedHotSpot = hotSpotService.createHotSpot(safeRequest)
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
        val currentLocalId = requireCurrentLocalId()
        if (existing.userId != null && existing.userId != currentLocalId) {
            throw AccessDeniedException("No puedes editar un punto de peligro de otro usuario")
        }
        // Igual que en createHotSpot: el userId final del reporte es siempre el
        // del usuario autenticado, nunca el que venga en el body (evita que se
        // reasigne un reporte propio a otro userId arbitrario).
        val safeRequest = request.copy(userId = currentLocalId)
        return ResponseEntity.ok(hotSpotService.updateHotSpot(id, safeRequest))
    }

    // Antes este endpoint no verificaba dueño en absoluto: cualquier usuario
    // autenticado podia desactivar (silenciar) el reporte de peligro de
    // cualquier otro usuario. Ahora sigue la misma regla que update/delete.
    @PutMapping("/{id}/deactivate")
    fun deactivateHotSpot(@PathVariable id: Long): ResponseEntity<HotSpotResponse> {
        val existing = hotSpotService.getHotSpotById(id)
        val currentLocalId = requireCurrentLocalId()
        if (existing.userId != null && existing.userId != currentLocalId) {
            throw AccessDeniedException("No puedes desactivar un punto de peligro de otro usuario")
        }
        return ResponseEntity.ok(hotSpotService.deactivateHotSpot(id))
    }

    @DeleteMapping("/{id}")
    fun deleteHotSpot(@PathVariable id: Long): ResponseEntity<Void> {
        val existing = hotSpotService.getHotSpotById(id)
        val currentLocalId = requireCurrentLocalId()
        if (existing.userId != null && existing.userId != currentLocalId) {
            throw AccessDeniedException("No puedes eliminar un punto de peligro de otro usuario")
        }
        hotSpotService.deleteHotSpot(id)
        return ResponseEntity.noContent().build()
    }

    // Centraliza la resolucion del usuario local a partir del JWT. Si el token
    // es valido pero todavia no hay fila local sincronizada (caso raro: JWT
    // valido sin paso previo por /api/users/sync), se rechaza en vez de dejar
    // pasar un userId nulo o inventado.
    private fun requireCurrentLocalId(): Long {
        return userService.resolveLocalId(CurrentUser.sub())
            ?: throw AccessDeniedException("No se pudo verificar tu usuario. Vuelve a iniciar sesión.")
    }
}