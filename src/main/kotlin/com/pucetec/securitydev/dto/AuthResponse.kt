
package com.pucetec.securitydev.dto

data class AuthResponse(
    val token: String,
    val userId: Long,
    val name: String
)