package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.DashboardResponse
import com.pucetec.securitydev.dto.UserAdminResponse
import com.pucetec.securitydev.dto.UserCreateRequest
import com.pucetec.securitydev.dto.UserUpdateRequest
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val hotSpotRepository: HotSpotRepository,
    private val locationShareRepository: LocationShareRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun getAllUsers(): List<UserAdminResponse> {
        return userRepository.findAll().map { toUserAdminResponse(it) }
    }

    fun getUserById(id: Long): UserAdminResponse {
        val user = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        return toUserAdminResponse(user)
    }

    // ── Crear usuario (nuevo) ──────────────────────────────────────
    // A diferencia de UserService.registerUser (usado por el registro público),
    // este método lo invoca un admin desde el panel: aquí SÍ se define una
    // contraseña inicial explícita en vez de que el usuario la elija.
    fun createUser(request: UserCreateRequest): UserAdminResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Ya existe un usuario registrado con ese correo")
        }

        val newUser = Users(
            id = 0, // 0 le indica a Hibernate/JPA que genere el ID automáticamente al insertar
            name = request.name,
            email = request.email,
            number = request.number,
            password = passwordEncoder.encode(request.password),
            hotSpots = mutableListOf()
        )
        return toUserAdminResponse(userRepository.save(newUser))
    }

    fun updateUser(id: Long, request: UserUpdateRequest): UserAdminResponse {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        // hotSpots = existing.hotSpots preserva la relación —
        // si no se pasa, Hibernate interpreta que se los quitaron todos y los BORRA (orphanRemoval).
        val updated = Users(
            id = existing.id,
            name = request.name,
            email = request.email,
            number = request.number,
            password = existing.password,
            hotSpots = existing.hotSpots
        )
        return toUserAdminResponse(userRepository.save(updated))
    }

    fun resetPassword(id: Long, newPassword: String) {
        val existing = userRepository.findById(id).orElseThrow {
            RuntimeException("Usuario no encontrado con ID: $id")
        }
        val updated = Users(
            id = existing.id,
            name = existing.name,
            email = existing.email,
            number = existing.number,
            password = passwordEncoder.encode(newPassword),
            hotSpots = existing.hotSpots
        )
        userRepository.save(updated)
    }

    fun deleteUser(id: Long) {
        if (!userRepository.existsById(id)) {
            throw RuntimeException("Usuario no encontrado con ID: $id")
        }
        // Sin esto, el DELETE falla por la foreign key de location_share → users
        locationShareRepository.deleteByUsersId(id)
        // Los hotspots del usuario se borran solos (cascade + orphanRemoval en Users.hotSpots)
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