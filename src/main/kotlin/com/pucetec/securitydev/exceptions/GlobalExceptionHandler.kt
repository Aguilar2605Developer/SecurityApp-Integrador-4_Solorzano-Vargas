package com.pucetec.securitydev.exceptions

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import software.amazon.awssdk.awscore.exception.AwsServiceException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(HotSpotNotFoundException::class)
    fun handleHotSpotNotFound(ex: HotSpotNotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(VerificationNotFoundException::class)
    fun handleVerificationNotFound(ex: VerificationNotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(LocationShareNotFoundException::class)
    fun handleLocationShareNotFound(ex: LocationShareNotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException): ResponseEntity<String> {
        logger.warn("JSON invalido o incompleto", ex)
        return ResponseEntity("El cuerpo de la solicitud esta mal formado o incompleto", HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String?>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<String> {
        return ResponseEntity("El parametro '${ex.name}' tiene un formato invalido", HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<String> {
        logger.warn("Argumento invalido: {}", ex.message)
        return ResponseEntity(ex.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<String> {
        logger.warn("Acceso denegado: {}", ex.message)
        return ResponseEntity(ex.message, HttpStatus.FORBIDDEN)
    }

    @ExceptionHandler(AwsServiceException::class)
    fun handleAwsServiceException(ex: AwsServiceException): ResponseEntity<Map<String, String?>> {
        val details = ex.awsErrorDetails()
        logger.error(
            "AWS service exception. statusCode={}, requestId={}, errorCode={}, errorMessage={}",
            ex.statusCode(),
            ex.requestId(),
            details?.errorCode(),
            details?.errorMessage(),
            ex
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf(
                "error" to "Error llamando a AWS",
                "awsErrorCode" to details?.errorCode(),
                "awsMessage" to details?.errorMessage(),
                "requestId" to ex.requestId()
            )
        )
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, String>> {
        logger.error("RuntimeException no controlada", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf("error" to (ex.message ?: "Error interno del servidor"))
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<Map<String, String>> {
        logger.error("Excepcion no controlada", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            mapOf("error" to (ex.message ?: "Error interno del servidor"))
        )
    }
}
