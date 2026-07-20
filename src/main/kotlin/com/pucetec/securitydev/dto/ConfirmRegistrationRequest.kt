package com.pucetec.securitydev.dto

data class ConfirmRegistrationRequest(
    val email: String,
    val code: String
)