package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["*"]) // Permite que tu app de Ionic se conecte sin bloqueos de CORS
class UserController(private val userService: UserService) {

    // Guarda un nuevo estudiante procesando su formulario de registro desde Ionic
    @PostMapping("/register")
    fun registerUser(@RequestBody request: UserRequest): ResponseEntity<UserResponse> {
        val createdUser = userService.registerUser(request)
        return ResponseEntity(createdUser, HttpStatus.CREATED)
    }

    // Devuelve la lista completa de usuarios registrados en el sistema
    @GetMapping
    fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(userService.getAllUsers())
    }

    // Busca un estudiante específico mediante su ID único
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Actualiza los datos de un estudiante existente
    @PutMapping("/{id}")
    fun updateUser(@PathVariable id: Long, @RequestBody request: UserRequest): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUser(id, request)
        return if (updatedUser != null) {
            ResponseEntity.ok(updatedUser)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // Elimina un estudiante por su ID
    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Void> {
        val deleted = userService.deleteUser(id)
        return if (deleted) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}