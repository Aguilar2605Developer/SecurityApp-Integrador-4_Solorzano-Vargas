package com.pucetec.securitydev.dto

data class UserCreateRequest(
    val name: String,
    val email: String,
    val number: String,
    val password: String // password TEMPORAL — Cognito obliga a cambiarla en el primer login
)