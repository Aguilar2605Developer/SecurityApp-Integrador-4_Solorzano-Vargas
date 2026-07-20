package com.pucetec.securitydev.service

import com.pucetec.securitydev.dto.UserRequest
import com.pucetec.securitydev.dto.UserResponse
import com.pucetec.securitydev.entity.Users
import com.pucetec.securitydev.mappers.UserMapper
import com.pucetec.securitydev.repository.LocationShareRecipientRepository
import com.pucetec.securitydev.repository.LocationShareRepository
import com.pucetec.securitydev.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val cognitoService: CognitoService,
    private val locationShareRepository: LocationShareRepository,
    private val locationShareRecipientRepository: LocationShareRecipientRepository
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun findOrCreateByCognitoSub(sub: String, email: String, name: String): UserResponse {
        val existing = userRepository.findByCognitoSub(sub)
        if (existing != null) return userMapper.toResponse(existing)

        val byEmail = userRepository.findByEmail(email)
        if (byEmail != null) {
            val updated = Users(
                id = byEmail.id,
                cognitoSub = sub,
                email = byEmail.email,
                name = byEmail.name.ifBlank { name },
                number = byEmail.number,
                hotSpotReports = byEmail.hotSpotReports
            )
            return userMapper.toResponse(userRepository.save(updated))
        }

        val created = userRepository.save(Users(cognitoSub = sub, email = email, name = name, number = ""))
        return userMapper.toResponse(created)
    }

    fun getAllUsers(): List<UserResponse> = userRepository.findAll().map { userMapper.toResponse(it) }

    fun getUserById(id: Long): UserResponse? =
        userRepository.findById(id).orElse(null)?.let { userMapper.toResponse(it) }

    fun updateUser(id: Long, request: UserRequest): UserResponse? {
        val existing = userRepository.findById(id).orElse(null) ?: return null
        val updated = Users(
            id = existing.id,
            cognitoSub = existing.cognitoSub,
            email = existing.email,
            name = request.name,
            number = request.number,
            hotSpotReports = existing.hotSpotReports
        )
        return userMapper.toResponse(userRepository.save(updated))
    }

    // Auto-borrado de cuenta (endpoint DELETE /api/users/{id}, el propio usuario
    // borrando su cuenta). Sigue el mismo orden que AdminService.deleteUser:
    // primero los destinatarios (location_share_recipient), luego los location_share,
    // luego Cognito, y al final la fila local -- si no, revienta la foreign key
    // igual que en el panel admin, y ademas dejaria la cuenta viva en Cognito.
    @Transactional
    fun deleteUser(id: Long): Boolean {
        val existing = userRepository.findById(id).orElse(null) ?: return false
        locationShareRecipientRepository.deleteByLocationShareUsersId(id)
        locationShareRepository.deleteByUsersId(id)
        try {
            cognitoService.deleteUserIfExists(existing.email)
        } catch (ex: Exception) {
            logger.warn("No se pudo borrar {} de Cognito durante el auto-borrado: {}", existing.email, ex.message)
        }
        userRepository.deleteById(id)
        return true
    }

    fun resolveLocalId(sub: String?): Long? {
        if (sub == null) return null
        return userRepository.findByCognitoSub(sub)?.id
    }

    /**
     * Paso 1 del registro: solo inicia el SignUp publico en Cognito.
     * El usuario local NO se crea aqui todavia -- se crea recien en
     * confirmRegistration(), una vez que Cognito confirma que el correo es real.
     */
    fun registerNewUser(email: String, name: String, number: String, password: String): UserResponse {
        val normalizedEmail = email.trim().lowercase()

        if (userRepository.existsByEmail(normalizedEmail)) {
            // La fila local puede ser una cuenta real ya confirmada, o puede ser un
            // registro huerfano: alguien borro el usuario directo desde la consola de
            // Cognito (en vez del boton "Eliminar usuario" del panel admin, que si
            // limpia ambos lados). Antes de bloquear el registro, confirmamos con
            // Cognito que la cuenta todavia existe de verdad.
            val cognitoStatus = cognitoService.getUserStatus(normalizedEmail)
            if (cognitoStatus == null) {
                logger.warn(
                    "El correo {} existe en la BD local pero ya no existe en Cognito. " +
                            "Se limpia el registro huerfano para permitir un registro nuevo.",
                    normalizedEmail
                )
                purgeOrphanedLocalUser(normalizedEmail)
            } else {
                throw IllegalArgumentException("Ya existe una cuenta con ese correo.")
            }
        }

        val cleanPassword = password.trim()
        if (cleanPassword.length < 8) {
            throw IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.")
        }

        cognitoService.signUpPublic(normalizedEmail, name, number, cleanPassword)
        logger.info("SignUp iniciado para {}. Pendiente de confirmacion con codigo.", normalizedEmail)

        return UserResponse(id = 0, name = name, email = normalizedEmail, number = number)
    }

    // Limpia una fila de 'users' cuyo cognitoSub ya no corresponde a ningun usuario
    // real en Cognito (borrado manual desde la consola de AWS). Respeta el mismo
    // orden que AdminService.deleteUser para no romper la foreign key de
    // location_share_recipient -> location_share.
    @Transactional
    fun purgeOrphanedLocalUser(email: String) {
        val orphan = userRepository.findByEmail(email) ?: return
        locationShareRecipientRepository.deleteByLocationShareUsersId(orphan.id)
        locationShareRepository.deleteByUsersId(orphan.id)
        userRepository.delete(orphan)
    }

    /**
     * Paso 2 del registro: confirma el codigo que Cognito mando de verdad al
     * correo. Solo aqui se crea (o se completa) el usuario local.
     */
    @Transactional
    fun confirmRegistration(email: String, code: String): UserResponse {
        val normalizedEmail = email.trim().lowercase()

        try {
            cognitoService.confirmSignUpPublic(normalizedEmail, code)
        } catch (e: Exception) {
            logger.warn("Fallo la confirmacion de registro para {}: {}", normalizedEmail, e.message)
            throw e
        }

        try {
            cognitoService.addUserToGroup(normalizedEmail, "USER")
        } catch (groupError: Exception) {
            logger.error(
                "No se pudo agregar {} al grupo USER. El registro continua porque el correo ya fue confirmado.",
                normalizedEmail,
                groupError
            )
        }

        val sub = cognitoService.getUserSub(normalizedEmail)
        val attributes = cognitoService.getUserAttributes(normalizedEmail)
        val name = attributes["name"] ?: normalizedEmail

        val existingByEmail = userRepository.findByEmail(normalizedEmail)
        val saved = if (existingByEmail != null) {
            userRepository.save(
                Users(
                    id = existingByEmail.id,
                    cognitoSub = sub,
                    email = normalizedEmail,
                    name = name,
                    number = existingByEmail.number,
                    hotSpotReports = existingByEmail.hotSpotReports
                )
            )
        } else {
            userRepository.save(
                Users(cognitoSub = sub, email = normalizedEmail, name = name, number = "")
            )
        }

        logger.info("Usuario {} confirmado y sincronizado localmente (id={})", normalizedEmail, saved.id)
        return userMapper.toResponse(saved)
    }

    fun resendConfirmationCode(email: String) {
        cognitoService.resendConfirmationCode(email.trim().lowercase())
    }
}