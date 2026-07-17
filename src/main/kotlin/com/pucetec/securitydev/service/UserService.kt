package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.mappers.UserMapper
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper
) {

    fun findOrCreateByCognitoSub(sub: String, email: String, name: String): UserResponse {
        val existing = userRepository.findByCognitoSub(sub)
        if (existing != null) return userMapper.toResponse(existing)
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
}