package com.pucetec.securitydev.dto

data class RegisterRequest(
    val email: String,
    val name: String,
    val number: String,
    val password: String? = null
)
