package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.*
import com.pucetec.securitydev.service.AdminService
import com.pucetec.securitydev.service.HotSpotService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = ["*"])
class AdminController(
    private val adminService: AdminService,
    private val hotSpotService: HotSpotService
) {

    @GetMapping("/dashboard")
    fun getDashboard(): ResponseEntity<DashboardResponse> {
        return ResponseEntity.ok(adminService.getDashboardStats())
    }

    @GetMapping("/users")
    fun getAllUsers(): ResponseEntity<List<UserAdminResponse>> {
        return ResponseEntity.ok(adminService.getAllUsers())
    }

    @GetMapping("/users/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserAdminResponse> {
        return ResponseEntity.ok(adminService.getUserById(id))
    }

    // ── NUEVO: crear usuario desde el panel de admin ──
    @PostMapping("/users")
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<UserAdminResponse> {
        return ResponseEntity(adminService.createUser(request), HttpStatus.CREATED)
    }

    @PutMapping("/users/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody request: UserUpdateRequest): ResponseEntity<UserAdminResponse> {
        return ResponseEntity.ok(adminService.updateUser(id, request))
    }

    @PutMapping("/users/{id}/reset-password")
    fun resetPassword(@PathVariable id: Long, @RequestBody request: ResetPasswordRequest): ResponseEntity<String> {
        adminService.resetPassword(id, request.newPassword)
        return ResponseEntity.ok("Contraseña actualizada")
    }

    @DeleteMapping("/users/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        adminService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }

    // Limpieza masiva de usuarios huerfanos (borrados manualmente desde la
    // consola de Cognito). Uso puntual: Postman/curl, no hace falta boton en el front.
    @PostMapping("/users/purge-orphans")
    fun purgeOrphanedUsers(): ResponseEntity<Map<String, Any>> {
        val removed = adminService.purgeOrphanedUsers()
        return ResponseEntity.ok(
            mapOf(
                "message" to "${removed.size} usuario(s) huerfano(s) eliminado(s)",
                "removedEmails" to removed
            )
        )
    }

    // Trae desde Cognito los usuarios confirmados que no tienen fila local
    // (BD reseteada, usuario creado directo desde la consola de AWS, etc.)
    // y los crea. Es la solucion definitiva a la desincronizacion: se puede
    // llamar cuantas veces sea necesario, solo crea lo que falta.
    @PostMapping("/users/sync-from-cognito")
    fun syncUsersFromCognito(): ResponseEntity<SyncFromCognitoResponse> {
        return ResponseEntity.ok(adminService.syncUsersFromCognito())
    }

    @GetMapping("/hotspots")
    fun getAllHotSpots(): ResponseEntity<List<HotSpotResponse>> {
        return ResponseEntity.ok(hotSpotService.getAllHotSpotsAdmin())
    }

    @PostMapping("/hotspots")
    fun createHotSpot(@RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        return ResponseEntity(hotSpotService.createHotSpot(request), HttpStatus.CREATED)
    }

    @PutMapping("/hotspots/{id}")
    fun updateHotSpot(@PathVariable id: Long, @RequestBody request: HotSpotRequest): ResponseEntity<HotSpotResponse> {
        return ResponseEntity.ok(hotSpotService.updateHotSpot(id, request))
    }

    @PutMapping("/hotspots/{id}/deactivate")
    fun deactivateHotSpot(@PathVariable id: Long): ResponseEntity<HotSpotResponse> {
        return ResponseEntity.ok(hotSpotService.deactivateHotSpot(id))
    }

    @DeleteMapping("/hotspots/{id}")
    fun deleteHotSpot(@PathVariable id: Long): ResponseEntity<Void> {
        hotSpotService.deleteHotSpot(id)
        return ResponseEntity.noContent().build()
    }
}