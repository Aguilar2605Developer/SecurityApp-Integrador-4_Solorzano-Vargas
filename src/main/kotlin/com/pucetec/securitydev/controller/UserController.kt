package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.security.CurrentUser
import com.pucetec.securitydev.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["*"])
class UserController(private val userService: UserService) {

    // Se llama justo después del login contra Cognito para crear/traer el perfil local
    @PostMapping("/sync")
    fun syncProfile(): ResponseEntity<UserResponse> {
        val sub = CurrentUser.sub() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val email = CurrentUser.email() ?: ""
        val name = CurrentUser.name() ?: email
        return ResponseEntity.ok(userService.findOrCreateByCognitoSub(sub, email, name))
    }

    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> = ResponseEntity.ok(userService.getAllUsers())

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return if (user != null) ResponseEntity.ok(user) else ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody request: UserRequest): ResponseEntity<UserResponse> {
        val currentLocalId = userService.resolveLocalId(CurrentUser.sub())
        if (currentLocalId != null && currentLocalId != id) {
            throw AccessDeniedException("No puedes editar el perfil de otro usuario")
        }
        val updatedUser = userService.updateUser(id, request)
        return if (updatedUser != null) ResponseEntity.ok(updatedUser) else ResponseEntity.notFound().build()
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        val currentLocalId = userService.resolveLocalId(CurrentUser.sub())
        if (currentLocalId != null && currentLocalId != id) {
            throw AccessDeniedException("No puedes eliminar la cuenta de otro usuario")
        }
        val deleted = userService.deleteUser(id)
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }
}