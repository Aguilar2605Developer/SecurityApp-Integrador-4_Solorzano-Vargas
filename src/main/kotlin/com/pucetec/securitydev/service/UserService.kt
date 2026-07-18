package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.mappers.UserMapper
import com.pucetec.securitydev.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val cognitoService: CognitoService
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun findOrCreateByCognitoSub(sub: String, email: String, name: String): UserResponse {
        val existing = userRepository.findByCognitoSub(sub)
        if (existing != null) return userMapper.toResponse(existing)

        val byEmail = userRepository.findByEmail(email)
        if (byEmail != null) {
            val updated = Users(
                id = byEmail.id,
                cognitoSub = sub,
                email = byEmail.email,
                name = byEmail.name.ifBlank { name },
                number = byEmail.number,
                hotSpots = byEmail.hotSpots
            )
            return userMapper.toResponse(userRepository.save(updated))
        }

        val created = userRepository.save(Users(cognitoSub = sub, email = email, name = name, number = ""))
        return userMapper.toResponse(created)
    }

    fun getAllUsers(): List<UserResponse> = userRepository.findAll().map { userMapper.toResponse(it) }

    fun getUserById(id: Long): UserResponse? =
        userRepository.findById(id).orElse(null)?.let { userMapper.toResponse(it) }

    fun updateUser(id: Long, request: UserRequest): UserResponse? {
        val existing = userRepository.findById(id).orElse(null) ?: return null
        val updated = Users(
            id = existing.id,
            cognitoSub = existing.cognitoSub,
            email = existing.email,
            name = request.name,
            number = request.number,
            hotSpots = existing.hotSpots
        )
        return userMapper.toResponse(userRepository.save(updated))
    }

    fun deleteUser(id: Long): Boolean {
        if (!userRepository.existsById(id)) return false
        userRepository.deleteById(id)
        return true
    }

    fun resolveLocalId(sub: String?): Long? {
        if (sub == null) return null
        return userRepository.findByCognitoSub(sub)?.id
    }

    @Transactional
    fun registerNewUser(email: String, name: String, number: String, password: String? = null): UserResponse {
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("Ya existe una cuenta con ese correo.")
        }

        var shouldRollbackCognitoUser = false
        try {
            val cognitoSub = if (password.isNullOrBlank()) {
                logger.warn(
                    "Registro de {} recibido sin password. Se usa el flujo legacy: el frontend ya hizo signUp y Cognito puede haber enviado correo de verificacion.",
                    email
                )
                cognitoService.confirmSignUp(email)
                cognitoService.getUserSub(email)
            } else {
                val cleanPassword = password.trim()

                if (cleanPassword.length < 8) {
                    throw IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.")
                }

                logger.info(
                    "Password recibido para {}. longitudOriginal={}, longitudLimpia={}",
                    email,
                    password.length,
                    cleanPassword.length
                )

                shouldRollbackCognitoUser = true
                cognitoService.createConfirmedUser(email, name, cleanPassword)
            }

            try {
                cognitoService.addUserToGroup(email, "USER")
            } catch (groupError: Exception) {
                logger.error(
                    "No se pudo agregar {} al grupo USER. El registro continua porque el usuario ya fue creado y confirmado en Cognito.",
                    email,
                    groupError
                )
            }

            val savedUser = userRepository.save(
                Users(
                    cognitoSub = cognitoSub,
                    email = email,
                    name = name,
                    number = number
                )
            )
            return userMapper.toResponse(savedUser)
        } catch (e: Exception) {
            logger.error(
                "Fallo el registro para {}. Se revierte Cognito si el backend creo el usuario.",
                email,
                e
            )
            if (shouldRollbackCognitoUser) {
                cognitoService.deleteUserIfExists(email)
            }
            throw RuntimeException("No se pudo completar el registro. ${e.message}", e)
        }
    }
}