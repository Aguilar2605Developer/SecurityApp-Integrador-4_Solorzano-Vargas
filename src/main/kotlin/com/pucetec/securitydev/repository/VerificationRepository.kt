package com.pucetec.securitydev.repository
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.entity.Verification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VerificationRepository: JpaRepository<Verification, Long>