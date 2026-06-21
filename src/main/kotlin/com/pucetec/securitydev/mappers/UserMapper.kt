package com.pucetec.securitydev.mappers

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.entity.Users
import org.springframework.stereotype.Component

@Component
class UserMapper {

    // 1. De la App a la Base de Datos: Procesa los datos incluyendo el password
    fun toEntity(request: UserRequest, id: Long = 0L): Users {
        return Users(
            id = id,
            name = request.name,
            email = request.email,
            number = request.number,
            password = request.password
        )
    }

    // 2. De la Base de Datos a la App: Filtra el password por seguridad
    fun toResponse(user: Users): UserResponse {
        return UserResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            number = user.number
        )
    }
}