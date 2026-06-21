package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.mappers.UserMapper
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val passwordEncoder: PasswordEncoder
) {

    fun registerUser(request: UserRequest): UserResponse {
        val encodedRequest = request.copy(password = passwordEncoder.encode(request.password))
        val userEntity = userMapper.toEntity(encodedRequest)
        val savedUser = userRepository.save(userEntity)
        return userMapper.toResponse(savedUser)
    }

    fun getAllUsers(): List<UserResponse> {
        return userRepository.findAll().map { user ->
            userMapper.toResponse(user)
        }
    }

    fun getUserById(id: Long): UserResponse? {
        val user = userRepository.findById(id).orElse(null) ?: return null
        return userMapper.toResponse(user)
    }

    fun updateUser(id: Long, request: UserRequest): UserResponse? {
        if (!userRepository.existsById(id)) return null
        val encodedRequest = request.copy(password = passwordEncoder.encode(request.password))
        val updatedEntity = userMapper.toEntity(encodedRequest, id)
        val savedUser = userRepository.save(updatedEntity)
        return userMapper.toResponse(savedUser)
    }

    fun deleteUser(id: Long): Boolean {
        if (!userRepository.existsById(id)) return false
        userRepository.deleteById(id)
        return true
    }
}