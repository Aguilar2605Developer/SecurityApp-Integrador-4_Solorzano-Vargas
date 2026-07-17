package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.DashboardResponse
import com.pucetec.securitydev.dto.UserAdminResponse
import com.pucetec.securitydev.dto.UserCreateRequest
import com.pucetec.securitydev.dto.UserUpdateRequest
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val hotSpotRepository: HotSpotRepository,
    private val locationShareRepository: LocationShareRepository,
    private val cognitoAdminService: CognitoAdminService
) {

    fun getAllUsers(): List<UserAdminResponse> = userRepository.findAll().map { toUserAdminResponse(it) }

    fun getUserById(id: Long): UserAdminResponse {
        val user = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        return toUserAdminResponse(user)
    }

    // Crea el usuario primero en Cognito (fuente real de verdad de la identidad),
    // y solo si eso funciona, guarda el perfil local enlazado por 'sub'.
    fun createUser(request: UserCreateRequest): UserAdminResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Ya existe un usuario registrado con ese correo")
        }

        val sub = cognitoAdminService.createUser(request.email, request.name, request.password)
        cognitoAdminService.addUserToGroup(request.email, "USER")

        val newUser = Users(
            id = 0,
            cognitoSub = sub,
            name = request.name,
            email = request.email,
            number = request.number,
            hotSpots = mutableListOf()
        )
        return toUserAdminResponse(userRepository.save(newUser))
    }

    fun updateUser(id: Long, request: UserUpdateRequest): UserAdminResponse {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        val updated = Users(
            id = existing.id,
            cognitoSub = existing.cognitoSub,
            name = request.name,
            email = request.email,
            number = request.number,
            hotSpots = existing.hotSpots
        )
        return toUserAdminResponse(userRepository.save(updated))
    }

    // El reset de contraseña ahora pasa por Cognito, no se toca la base local
    fun resetPassword(id: Long, newPassword: String) {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        cognitoAdminService.resetPassword(existing.email, newPassword)
    }

    fun deleteUser(id: Long) {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        locationShareRepository.deleteByUsersId(id)
        cognitoAdminService.deleteUser(existing.email)
        userRepository.deleteById(id)
    }

    fun getDashboardStats(): DashboardResponse {
        val activeHotspots = hotSpotRepository.findByActiveTrue()
        val hotspotsByModality = activeHotspots.groupingBy { it.modality }.eachCount()

        return DashboardResponse(
            totalUsers = userRepository.count().toInt(),
            activeHotspotsTotal = activeHotspots.size,
            hotspotsByModality = hotspotsByModality,
            activeShares = locationShareRepository.countByActiveTrue().toInt()
        )
    }

    private fun toUserAdminResponse(user: Users): UserAdminResponse {
        return UserAdminResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            number = user.number,
            hotspotsCount = user.hotSpots.size
        )
    }
}