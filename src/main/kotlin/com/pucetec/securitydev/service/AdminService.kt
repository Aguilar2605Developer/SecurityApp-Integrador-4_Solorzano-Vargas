package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.DashboardResponse
import com.pucetec.securitydev.dto.SyncFromCognitoResponse
import com.pucetec.securitydev.dto.UserAdminResponse
import com.pucetec.securitydev.dto.UserCreateRequest
import com.pucetec.securitydev.dto.UserUpdateRequest
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.repository.HotSpotRepository
import com.pucetec.securitydev.repository.HotSpotReportRepository
import com.pucetec.securitydev.repository.LocationShareRecipientRepository
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val hotSpotRepository: HotSpotRepository,
    private val hotSpotReportRepository: HotSpotReportRepository,
    private val locationShareRepository: LocationShareRepository,
    private val locationShareRecipientRepository: LocationShareRecipientRepository,
    private val cognitoAdminService: CognitoAdminService,
    private val cognitoService: CognitoService
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
        // Primero hay que romper la referencia de los destinatarios (location_share_recipient)
        // antes de poder borrar los location_share del usuario, o Postgres rechaza el DELETE
        // por la foreign key (fk...meareawgr).
        locationShareRecipientRepository.deleteByLocationShareUsersId(id)
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

    // Limpieza masiva: recorre todos los usuarios locales y borra (BD local, en
    // cascada segura) los que ya no existen de verdad en Cognito -- el caso de
    // cuando alguien borro usuarios directo desde la consola de AWS en vez de
    // usar el boton "Eliminar usuario" de este panel. Devuelve los correos
    // que se limpiaron para que quede registro de que paso.
    @Transactional
    fun purgeOrphanedUsers(): List<String> {
        val allUsers = userRepository.findAll()
        val removedEmails = mutableListOf<String>()

        for (user in allUsers) {
            val cognitoStatus = cognitoService.getUserStatus(user.email)
            if (cognitoStatus == null) {
                logger.warn(
                    "Usuario huerfano detectado: {} (id={}) ya no existe en Cognito. Se borra localmente.",
                    user.email, user.id
                )
                locationShareRecipientRepository.deleteByLocationShareUsersId(user.id)
                locationShareRepository.deleteByUsersId(user.id)
                userRepository.deleteById(user.id)
                removedEmails.add(user.email)
            }
        }

        return removedEmails
    }

    // Camino inverso a purgeOrphanedUsers(): en vez de borrar filas locales que
    // ya no existen en Cognito, trae desde Cognito los usuarios que SI existen
    // (confirmados) pero que no tienen fila local -- el caso de un reseteo de
    // BD, o de un usuario creado directo desde la consola de AWS. Es idempotente:
    // se puede llamar las veces que sea, solo crea lo que falta.
    @Transactional
    fun syncUsersFromCognito(): SyncFromCognitoResponse {
        val cognitoUsers = cognitoService.listAllUsers()
        val creados = mutableListOf<String>()
        val omitidos = mutableListOf<String>()
        var yaExistian = 0

        for (cognitoUser in cognitoUsers) {
            // Solo se sincronizan cuentas confirmadas -- una cuenta UNCONFIRMED
            // todavia no completo el registro (nadie valido que el correo es
            // real), asi que no debe aparecer como usuario del sistema.
            if (cognitoUser.status != "CONFIRMED") {
                omitidos.add(cognitoUser.email)
                continue
            }

            val existing = userRepository.findByCognitoSub(cognitoUser.sub)
                ?: userRepository.findByEmail(cognitoUser.email)

            if (existing != null) {
                yaExistian++
                continue
            }

            val newUser = Users(
                id = 0,
                cognitoSub = cognitoUser.sub,
                name = cognitoUser.name,
                email = cognitoUser.email,
                number = "",
                hotSpotReports = mutableListOf()
            )
            userRepository.save(newUser)
            creados.add(cognitoUser.email)
            logger.info("Usuario {} sincronizado desde Cognito (creado en BD local)", cognitoUser.email)
        }

        return SyncFromCognitoResponse(
            totalEnCognito = cognitoUsers.size,
            creados = creados,
            yaExistian = yaExistian,
            omitidosNoConfirmados = omitidos
        )
    }

    // Corre solo, cada 15 minutos, sin que nadie tenga que llamar al endpoint
    // manual. Asi, si alguien borra un usuario directo desde la consola de
    // Cognito (en vez del boton del panel), la fila local se limpia sola en
    // vez de quedar como fantasma indefinidamente.
    @Scheduled(fixedRate = 900000)
    fun autoPurgeOrphanedUsersJob() {
        try {
            val removed = purgeOrphanedUsers()
            if (removed.isNotEmpty()) {
                logger.info("Auto-limpieza de usuarios huerfanos: {} eliminado(s) -> {}", removed.size, removed)
            }
        } catch (ex: Exception) {
            // Si Cognito no responde (rate limit, red, etc.) no queremos tumbar
            // el scheduler; se reintenta solo en la siguiente corrida.
            logger.error("Fallo la auto-limpieza de usuarios huerfanos, se reintenta en el siguiente ciclo", ex)
        }
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