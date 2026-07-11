package com.pucetec.securitydev.repository

import com.pucetec.securitydev.entity.LocationShare
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface LocationShareRepository : JpaRepository<LocationShare, Long> {
    fun findByShareIdAndActiveTrue(shareId: String): LocationShare?
    fun findByShareId(shareId: String): LocationShare?
    fun findByActiveTrueAndExpiresAtBefore(dateTime: LocalDateTime): List<LocationShare>
    fun deleteByUsersId(userId: Long)
    fun countByActiveTrue(): Long
}