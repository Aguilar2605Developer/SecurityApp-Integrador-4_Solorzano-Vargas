package com.pucetec.securitydev.repository

import com.pucetec.securitydev.entity.Users
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<Users, Long> {
    fun findByEmail(email: String): Users?
    fun existsByEmail(email: String): Boolean
    fun findByCognitoSub(cognitoSub: String): Users?
}