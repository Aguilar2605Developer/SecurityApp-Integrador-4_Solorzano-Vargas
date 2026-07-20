package com.pucetec.securitydev.repository

import com.pucetec.securitydev.entity.LocationShareRecipient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface LocationShareRecipientRepository : JpaRepository<LocationShareRecipient, Long> {
    fun existsByLocationShareShareIdAndEmail(shareId: String, email: String): Boolean
    fun findByLocationShareShareId(shareId: String): List<LocationShareRecipient>
    fun deleteByLocationShareShareIdAndEmail(shareId: String, email: String)
    fun deleteByLocationShareUsersId(userId: Long)
}