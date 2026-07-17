package com.pucetec.securitydev.dto

import java.time.LocalDateTime

class LocationShareResponse(
    val shareId: String,
    val latitude: Double,
    val longitude: Double,
    val username: String?,
    val active: Boolean,
    val expiresAt: LocalDateTime,
    val userId: Long? = null
)