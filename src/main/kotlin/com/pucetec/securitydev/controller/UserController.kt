package com.pucetec.securitydev.controller

import com.pucetec.securitydev.dto.ConfirmRegistrationRequest
import com.pucetec.securitydev.dto.RegisterRequest
import com.pucetec.securitydev.dto.ResendCodeRequest
import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.security.CurrentUser
import com.pucetec.securitydev.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = ["*"])
class UserController(private val userService: UserService) {

    private val logger = LoggerFactory.getLogger(UserController::class.java)

    @PostMapping("/sync")
    fun syncProfile(): ResponseEntity<UserResponse> {
        val sub = CurrentUser.sub() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val email = CurrentUser.email() ?: ""
        val name = CurrentUser.name() ?: email
        return ResponseEntity.ok(userService.findOrCreateByCognitoSub(sub, email, name))
    }

    // Paso 1: inicia el SignUp publico en Cognito. Cognito manda el codigo real.
    @PostMapping("/register")
    fun registerUser(@RequestBody request: RegisterRequest): ResponseEntity<Any> {
        return try {
            val userResponse = userService.registerNewUser(request.email, request.name, request.number, request.password)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "message" to "Cuenta creada. Revisa tu correo e ingresa el codigo para confirmarla.",
                    "user" to userResponse
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Registro rechazado para {}: {}", request.email, e.message)
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error procesando POST /api/users/register para {}", request.email, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("error" to (e.message ?: "Error interno del servidor"))
            )
        }
    }

    // Paso 2: confirma con el codigo real que llego al correo. Aqui se crea el usuario local.
    @PostMapping("/confirm")
    fun confirmRegistration(@RequestBody request: ConfirmRegistrationRequest): ResponseEntity<Any> {
        return try {
            val userResponse = userService.confirmRegistration(request.email, request.code)
            ResponseEntity.ok(
                mapOf(
                    "message" to "Cuenta confirmada. Ya puedes iniciar sesion.",
                    "user" to userResponse
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.warn("Confirmacion rechazada para {}: {}", request.email, e.message)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("error" to e.message))
        } catch (e: Exception) {
            logger.error("Error procesando POST /api/users/confirm para {}", request.email, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("error" to (e.message ?: "Error interno del servidor"))
            )
        }
    }

    @PostMapping("/resend-code")
    fun resendCode(@RequestBody request: ResendCodeRequest): ResponseEntity<Any> {
        return try {
            userService.resendConfirmationCode(request.email)
            ResponseEntity.ok(mapOf("message" to "Codigo reenviado."))
        } catch (e: Exception) {
            logger.error("Error reenviando codigo para {}", request.email, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf("error" to (e.message ?: "Error interno del servidor"))
            )
        }
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