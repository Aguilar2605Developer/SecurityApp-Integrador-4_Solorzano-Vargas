package com.pucetec.securitydev.dto

data class UserCreateRequest(
    val name: String,
    val email: String,
    val number: String,
    val password: String
)