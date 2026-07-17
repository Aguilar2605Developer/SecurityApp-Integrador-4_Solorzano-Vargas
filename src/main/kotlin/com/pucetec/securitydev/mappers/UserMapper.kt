package com.pucetec.securitydev.mappers

import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.entity.Users
import org.springframework.stereotype.Component

@Component
class UserMapper {

    fun toResponse(user: Users) = UserResponse(
        id = user.id,
        name = user.name,
        email = user.email,
        number = user.number
    )
}