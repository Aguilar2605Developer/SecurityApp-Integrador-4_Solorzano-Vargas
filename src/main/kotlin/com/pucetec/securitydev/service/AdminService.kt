package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.DashboardResponse
import com.pucetec.securitydev.dto.UserAdminResponse
import com.pucetec.securitydev.dto.UserCreateRequest
import com.pucetec.securitydev.dto.UserUpdateRequest
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.HotSpotReportRepository
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val hotSpotRepository: HotSpotRepository,
    private val hotSpotReportRepository: HotSpotReportRepository,
    private val locationShareRepository: LocationShareRepository,
    private val cognitoAdminService: CognitoAdminService
) {

    private val logger = LoggerFactory.getLogger(AdminService::class.java)

    fun getAllUsers(): List<UserAdminResponse> = userRepository.findAll().map { toUserAdminResponse(it) }

    fun getUserById(id: Long): UserAdminResponse {
        val user = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        return toUserAdminResponse(user)
    }

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
            hotSpotReports = mutableListOf()
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
            hotSpotReports = existing.hotSpotReports
        )
        return toUserAdminResponse(userRepository.save(updated))
    }

    fun resetPassword(id: Long, newPassword: String) {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        cognitoAdminService.resetPassword(existing.email, newPassword)
    }

    @Transactional
    fun deleteUser(id: Long) {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        locationShareRepository.deleteByUsersId(id)
        try {
            cognitoAdminService.deleteUser(existing.email)
        } catch (ex: UserNotFoundException) {
            // Ya no existe en Cognito (borrado manual, desincronización, etc.).
            // No es un motivo para bloquear la limpieza del registro local.
            logger.warn("Usuario '{}' no existe en Cognito, se omite y se continua con el borrado local", existing.email)
        }
        userRepository.deleteById(id)
    }

    fun getDashboardStats(): DashboardResponse {
        val activeHotspots = hotSpotRepository.findByActiveTrue()
        val activeIds = activeHotspots.map { it.id }
        val hotspotsByModality = hotSpotReportRepository.findByHotSpotIdIn(activeIds)
            .groupingBy { it.modality }
            .eachCount()

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
            hotspotsCount = user.hotSpotReports.size
        )
    }
}