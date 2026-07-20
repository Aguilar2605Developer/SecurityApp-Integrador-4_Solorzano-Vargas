package com.pucetec.securitydev.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ResendConfirmationCodeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUsersRequest

/** Resumen minimo de un usuario de Cognito, usado para la sincronizacion manual. */
data class CognitoUserSummary(
    val sub: String,
    val email: String,
    val name: String,
    val status: String,
    val enabled: Boolean
)

/**
 * IMPORTANTE — por que este servicio ya NO usa AdminCreateUser/AdminSetUserPassword
 * para el registro publico:
 *
 * La API de administracion (Admin*) permite que el propio backend marque
 * "email_verified = true" sin que Cognito le haya mandado nunca un codigo
 * al destinatario. Eso es util cuando un ADMIN humano crea una cuenta desde
 * el panel (ver CognitoAdminService), pero es peligroso para el registro
 * publico: cualquiera podria registrarse con el correo de otra persona y
 * el sistema lo daria por "verificado".
 *
 * Con SignUp/ConfirmSignUp publicos, es Cognito quien genera y envia el
 * codigo de verificacion directamente al correo real. Solo si el usuario
 * escribe ese codigo, email_verified pasa a true.
 */
@Service
class CognitoService(
    private val cognitoClient: CognitoIdentityProviderClient,
    @Value("\${cognito.user-pool-id}") private val userPoolId: String,
    @Value("\${cognito.client-id}") private val clientId: String,
    @Value("\${cognito.region}") private val region: String
) {

    private val logger = LoggerFactory.getLogger(CognitoService::class.java)

    /**
     * Inicia el registro publico. Cognito envia el codigo de verificacion
     * directamente al correo indicado. El usuario aun NO puede iniciar sesion
     * hasta que llame a confirmSignUpPublic con ese codigo.
     *
     * Devuelve el 'sub' del usuario (aunque todavia no este confirmado).
     */
    fun signUpPublic(email: String, name: String, phoneNumber: String, password: String): String {
        try {
            val request = SignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .password(password)
                .userAttributes(
                    AttributeType.builder().name("email").value(email).build(),
                    AttributeType.builder().name("name").value(name).build()
                )
                .build()

            val response = cognitoClient.signUp(request)
            logger.info("SignUp publico iniciado para {}. Cognito envio el codigo de verificacion.", email)
            return response.userSub()
        } catch (e: UsernameExistsException) {
            // El username ya existe en Cognito. Puede estar sin confirmar todavia
            // (por ejemplo si el usuario cerro la pestana antes de poner el codigo).
            val status = getUserStatus(email)
            if (status == "UNCONFIRMED") {
                logger.warn("El usuario {} ya existia sin confirmar. Se reenvia el codigo.", email)
                resendConfirmationCode(email)
                return getUserSub(email)
            }
            logger.warn("El usuario {} ya existe y esta confirmado en Cognito.", email)
            throw IllegalArgumentException("Ya existe una cuenta confirmada con ese correo. Intenta iniciar sesion.")
        } catch (e: CognitoIdentityProviderException) {
            logger.error("AWS Cognito fallo en SignUp. email={}, aws={}", email, awsErrorSummary(e), e)
            throw RuntimeException("No se pudo iniciar el registro: ${awsErrorMessage(e)}", e)
        } catch (e: SdkClientException) {
            logger.error("AWS SDK no pudo ejecutar SignUp. email={}", email, e)
            throw RuntimeException("No se pudo iniciar el registro: ${e.message}", e)
        }
    }

    /**
     * Confirma la cuenta con el codigo REAL que Cognito envio al correo.
     * Solo despues de esto email_verified pasa a true.
     */
    fun confirmSignUpPublic(email: String, code: String) {
        try {
            val request = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(code)
                .build()
            cognitoClient.confirmSignUp(request)
            logger.info("Usuario {} confirmo su correo con un codigo real de Cognito", email)
        } catch (e: CognitoIdentityProviderException) {
            logger.warn("Codigo de confirmacion invalido o expirado para {}: {}", email, awsErrorSummary(e))
            throw IllegalArgumentException("Codigo invalido o expirado: ${awsErrorMessage(e)}", e)
        } catch (e: SdkClientException) {
            logger.error("AWS SDK no pudo ejecutar ConfirmSignUp. email={}", email, e)
            throw RuntimeException("No se pudo confirmar la cuenta: ${e.message}", e)
        }
    }

    /** Por si el codigo expiro o el correo no llego. */
    fun resendConfirmationCode(email: String) {
        try {
            val request = ResendConfirmationCodeRequest.builder()
                .clientId(clientId)
                .username(email)
                .build()
            cognitoClient.resendConfirmationCode(request)
            logger.info("Codigo de confirmacion reenviado a {}", email)
        } catch (e: CognitoIdentityProviderException) {
            logger.error("No se pudo reenviar el codigo. email={}, aws={}", email, awsErrorSummary(e), e)
            throw RuntimeException("No se pudo reenviar el codigo: ${awsErrorMessage(e)}", e)
        }
    }

    /** Usado por AdminService/paneles: lectura administrativa, no fuerza verificacion de nada. */
    fun getUserSub(email: String): String {
        return getUserAttributes(email)["sub"]
            ?: throw IllegalStateException("Cognito no devolvio el atributo sub para $email")
    }

    fun getUserStatus(email: String): String? {
        return try {
            val request = AdminGetUserRequest.builder().userPoolId(userPoolId).username(email).build()
            cognitoClient.adminGetUser(request).userStatusAsString()
        } catch (e: UserNotFoundException) {
            null
        }
    }

    fun getUserAttributes(email: String): Map<String, String> {
        try {
            val request = AdminGetUserRequest.builder().userPoolId(userPoolId).username(email).build()
            val response = cognitoClient.adminGetUser(request)
            return response.userAttributes().associate { it.name() to it.value() }
        } catch (e: CognitoIdentityProviderException) {
            logger.error("AWS Cognito fallo en AdminGetUser. email={}, aws={}", email, awsErrorSummary(e), e)
            throw RuntimeException("No se pudo obtener el usuario de Cognito: ${awsErrorMessage(e)}", e)
        }
    }

    fun addUserToGroup(email: String, groupName: String) {
        try {
            val request = AdminAddUserToGroupRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .groupName(groupName)
                .build()
            cognitoClient.adminAddUserToGroup(request)
            logger.info("Usuario {} agregado al grupo {}", email, groupName)
        } catch (e: CognitoIdentityProviderException) {
            logger.error(
                "AWS Cognito fallo en AdminAddUserToGroup. email={}, group={}, aws={}",
                email, groupName, awsErrorSummary(e), e
            )
            throw RuntimeException("No se pudo asignar el grupo al usuario: ${awsErrorMessage(e)}", e)
        }
    }

    /** Rollback: borra un usuario de Cognito si algo falla despues del SignUp (confirmado o no). */
    fun deleteUserIfExists(email: String) {
        try {
            val request = AdminDeleteUserRequest.builder().userPoolId(userPoolId).username(email).build()
            cognitoClient.adminDeleteUser(request)
            logger.info("Usuario {} eliminado de Cognito por rollback", email)
        } catch (e: Exception) {
            logger.error("No se pudo eliminar usuario Cognito durante rollback. email={}", email, e)
        }
    }

    /**
     * Trae TODOS los usuarios del User Pool (pagina automaticamente con
     * paginationToken, Cognito devuelve maximo 60 por página). Se usa para
     * el boton "Sincronizar con Cognito" del panel admin: compara esta lista
     * contra la BD local y crea las filas que falten.
     */
    fun listAllUsers(): List<CognitoUserSummary> {
        val allUsers = mutableListOf<CognitoUserSummary>()
        var paginationToken: String? = null

        try {
            do {
                val requestBuilder = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(60)
                if (paginationToken != null) {
                    requestBuilder.paginationToken(paginationToken)
                }

                val response = cognitoClient.listUsers(requestBuilder.build())

                response.users().forEach { cognitoUser ->
                    val attributes = cognitoUser.attributes().associate { it.name() to it.value() }
                    val sub = attributes["sub"]
                    val email = attributes["email"]
                    if (sub != null && email != null) {
                        allUsers.add(
                            CognitoUserSummary(
                                sub = sub,
                                email = email,
                                name = attributes["name"] ?: email,
                                status = cognitoUser.userStatusAsString(),
                                enabled = cognitoUser.enabled()
                            )
                        )
                    }
                }

                paginationToken = response.paginationToken()
            } while (paginationToken != null)
        } catch (e: CognitoIdentityProviderException) {
            logger.error("AWS Cognito fallo en ListUsers. aws={}", awsErrorSummary(e), e)
            throw RuntimeException("No se pudo listar los usuarios de Cognito: ${awsErrorMessage(e)}", e)
        }

        return allUsers
    }

    private fun awsErrorMessage(e: CognitoIdentityProviderException): String {
        return e.awsErrorDetails()?.errorMessage() ?: e.message ?: e.javaClass.simpleName
    }

    private fun awsErrorSummary(e: CognitoIdentityProviderException): String {
        val details = e.awsErrorDetails()
        return "statusCode=${e.statusCode()}, requestId=${e.requestId()}, errorCode=${details?.errorCode()}, errorMessage=${details?.errorMessage()}"
    }
}