package com.pucetec.securitydev.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt

object CurrentUser {

    fun sub(): String? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return if (principal is Jwt) principal.subject else null
    }

    fun email(): String? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return if (principal is Jwt) principal.getClaimAsString("email") else null
    }

    fun name(): String? {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        return if (principal is Jwt) principal.getClaimAsString("name") else null
    }
}