package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.mappers.UserMapper
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {

    // Guarda un nuevo estudiante procesando su formulario de Ionic
    fun registerUser(request: UserRequest): UserResponse {
        // 1. Convertimos el paquete DTO que viene del celular en una Entidad de base de datos
        val userEntity = userMapper.toEntity(request)

        // 2. Persistimos los datos de forma segura en Neon Cloud
        val savedUser = userRepository.save(userEntity)

        // 3. Retornamos la respuesta limpia ocultando la contraseña por seguridad
        return userMapper.toResponse(savedUser)
    }

    // Devuelve la lista completa de usuarios registrados en el sistema
    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { user ->
            userMapper.toResponse(user)
        }
    }

    // Busca un estudiante específico mediante su ID único
    fun getUserById(id: Long): UserResponse? {
        val user = userRepository.findById(id).orElse(null) ?: return null
        return userMapper.toResponse(user)
    }
}