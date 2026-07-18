package com.pucetec.securitydev.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminConfirmSignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

@Service
class CognitoService(
    private val cognitoClient: CognitoIdentityProviderClient,
    @Value("\${cognito.user-pool-id}") private val userPoolId: String,
    @Value("\${cognito.region}") private val region: String
) {

    private val logger = LoggerFactory.getLogger(CognitoService::class.java)

    fun createConfirmedUser(email: String, name: String, password: String): String {
        require(password.trim().length >= 8) {
            "La contraseña temporal es invalida (longitud=${password.length}). Debe tener al menos 8 caracteres sin espacios al inicio/final."
        }

        try {
            val createRequest = AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .userAttributes(
                    AttributeType.builder().name("email").value(email).build(),
                    AttributeType.builder().name("email_verified").value("true").build(),
                    AttributeType.builder().name("name").value(name).build()
                )
                .temporaryPassword(password)
                .messageAction(MessageActionType.SUPPRESS)
                .build()

            val createResponse = cognitoClient.adminCreateUser(createRequest)
            val sub = createResponse.user().attributes()
                .firstOrNull { it.name() == "sub" }
                ?.value()
                ?: throw IllegalStateException("Cognito no devolvio el atributo sub para $email")

            val setPasswordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .password(password)
                .permanent(true)
                .build()

            cognitoClient.adminSetUserPassword(setPasswordRequest)
            logger.info("Usuario {} creado y confirmado en Cognito sin correo de verificacion", email)
            return sub
        } catch (e: UsernameExistsException) {
            logger.warn(
                "El usuario {} ya existe en Cognito. Se confirma y se fija password permanente; revisa si el frontend todavia llama signUp.",
                email,
                e
            )
            confirmExistingUserIfNeeded(email)
            setPermanentPassword(email, password)
            return getUserSub(email)
        } catch (e: CognitoIdentityProviderException) {
            logger.error(
                "AWS Cognito fallo creando usuario confirmado. email={}, userPoolId={}, region={}, aws={}",
                email,
                userPoolId,
                region,
                awsErrorSummary(e),
                e
            )
            throw RuntimeException("No se pudo crear el usuario en Cognito: ${awsErrorMessage(e)}", e)
        } catch (e: SdkClientException) {
            logger.error(
                "AWS SDK no pudo crear usuario confirmado. email={}, userPoolId={}, region={}",
                email,
                userPoolId,
                region,
                e
            )
            throw RuntimeException("No se pudo crear el usuario en Cognito: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error inesperado creando usuario confirmado en Cognito para {}", email, e)
            throw RuntimeException("No se pudo crear el usuario en Cognito: ${e.message}", e)
        }
    }

    private fun confirmExistingUserIfNeeded(email: String) {
        try {
            confirmSignUp(email)
        } catch (e: RuntimeException) {
            val cause = e.cause
            if (cause is NotAuthorizedException) {
                logger.info("Usuario {} ya estaba confirmado en Cognito", email)
                return
            }
            throw e
        }
    }

    private fun setPermanentPassword(email: String, password: String) {
        try {
            val request = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .password(password)
                .permanent(true)
                .build()
            cognitoClient.adminSetUserPassword(request)
            logger.info("Password permanente configurada para {}", email)
        } catch (e: CognitoIdentityProviderException) {
            logger.error(
                "AWS Cognito fallo en AdminSetUserPassword. email={}, userPoolId={}, region={}, aws={}",
                email,
                userPoolId,
                region,
                awsErrorSummary(e),
                e
            )
            throw RuntimeException("No se pudo configurar la password del usuario: ${awsErrorMessage(e)}", e)
        } catch (e: Exception) {
            logger.error("Error inesperado configurando password permanente para {}", email, e)
            throw RuntimeException("No se pudo configurar la password del usuario: ${e.message}", e)
        }
    }

    fun getUserSub(email: String): String {
        try {
            val request = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build()
            val response = cognitoClient.adminGetUser(request)
            return response.userAttributes()
                .firstOrNull { it.name() == "sub" }
                ?.value()
                ?: throw IllegalStateException("Cognito no devolvio el atributo sub para $email")
        } catch (e: CognitoIdentityProviderException) {
            logger.error(
                "AWS Cognito fallo en AdminGetUser. email={}, userPoolId={}, region={}, aws={}",
                email,
                userPoolId,
                region,
                awsErrorSummary(e),
                e
            )
            throw RuntimeException("No se pudo obtener el usuario de Cognito: ${awsErrorMessage(e)}", e)
        } catch (e: Exception) {
            logger.error("Error inesperado obteniendo usuario Cognito para {}", email, e)
            throw RuntimeException("No se pudo obtener el usuario de Cognito: ${e.message}", e)
        }
    }

    fun deleteUserIfExists(email: String) {
        try {
            val request = AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build()
            cognitoClient.adminDeleteUser(request)
            logger.info("Usuario {} eliminado de Cognito por rollback", email)
        } catch (e: CognitoIdentityProviderException) {
            logger.error(
                "No se pudo eliminar usuario Cognito durante rollback. email={}, userPoolId={}, region={}, aws={}",
                email,
                userPoolId,
                region,
                awsErrorSummary(e),
                e
            )
        } catch (e: Exception) {
            logger.error("No se pudo eliminar usuario Cognito durante rollback. email={}", email, e)
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
                "AWS Cognito fallo en AdminAddUserToGroup. email={}, group={}, userPoolId={}, region={}, aws={}",
                email,
                groupName,
                userPoolId,
                region,
                awsErrorSummary(e),
                e
            )
            throw RuntimeException("No se pudo asignar el grupo al usuario: ${awsErrorMessage(e)}", e)
        } catch (e: SdkClientException) {
            logger.error(
                "AWS SDK no pudo ejecutar AdminAddUserToGroup. email={}, group={}, userPoolId={}, region={}",
                email,
                groupName,
                userPoolId,
                region,
                e
            )
            throw RuntimeException("No se pudo asignar el grupo al usuario: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error inesperado al agregar usuario {} al grupo {}", email, groupName, e)
            throw RuntimeException("No se pudo asignar el grupo al usuario: ${e.message}", e)
        }
    }

    fun confirmSignUp(email: String) {
        try {
            val request = AdminConfirmSignUpRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .build()

            cognitoClient.adminConfirmSignUp(request)
            logger.info("Usuario {} confirmado automaticamente en Cognito", email)
        } catch (e: CognitoIdentityProviderException) {
            logger.error(
                "AWS Cognito fallo en AdminConfirmSignUp. email={}, userPoolId={}, region={}, aws={}",
                email,
                userPoolId,
                region,
                awsErrorSummary(e),
                e
            )
            throw RuntimeException("No se pudo confirmar la cuenta del usuario: ${awsErrorMessage(e)}", e)
        } catch (e: SdkClientException) {
            logger.error(
                "AWS SDK no pudo ejecutar AdminConfirmSignUp. email={}, userPoolId={}, region={}",
                email,
                userPoolId,
                region,
                e
            )
            throw RuntimeException("No se pudo confirmar la cuenta del usuario: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Error inesperado al confirmar automaticamente al usuario {}", email, e)
            throw RuntimeException("No se pudo confirmar la cuenta del usuario: ${e.message}", e)
        }
    }

    private fun awsErrorMessage(e: CognitoIdentityProviderException): String {
        return e.awsErrorDetails()?.errorMessage() ?: e.message ?: e.javaClass.simpleName
    }

    private fun awsErrorSummary(e: CognitoIdentityProviderException): String {
        val details = e.awsErrorDetails()
        return "statusCode=${e.statusCode()}, requestId=${e.requestId()}, errorCode=${details?.errorCode()}, errorMessage=${details?.errorMessage()}"
    }
}