package com.pucetec.securitydev.dto

data class UserAdminResponse(
    val id: Long,
    val name: String,
    val email: String,
    val number: String,
    val hotspotsCount: Int
)