package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.AuthRequest
import com.pucetec.securitydev.dto.AuthResponse
import com.pucetec.securitydev.repository.UserRepository
import com.pucetec.securitydev.security.JwtUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val jwtUtil: JwtUtil,
    private val passwordEncoder: PasswordEncoder
) {

    @Value("\${app.admin.email}")
    private lateinit var adminEmail: String

    fun login(request: AuthRequest): AuthResponse? {
        val user = userRepository.findByEmail(request.email) ?: return null
        if (!passwordEncoder.matches(request.password, user.password)) return null

        val token = jwtUtil.generateToken(user.email, user.id)
        return AuthResponse(
            token = token,
            userId = user.id,
            name = user.name,
            isAdmin = user.email.equals(adminEmail, ignoreCase = true)
        )
    }
}