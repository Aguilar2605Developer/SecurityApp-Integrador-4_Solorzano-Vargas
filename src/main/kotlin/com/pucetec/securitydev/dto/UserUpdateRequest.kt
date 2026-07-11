package com.pucetec.securitydev.dto

data class UserUpdateRequest(
    val name: String,
    val email: String,
    val number: String
)