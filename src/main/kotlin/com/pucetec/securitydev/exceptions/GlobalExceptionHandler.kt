package com.pucetec.securitydev.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {

    // 404 - Cuando un HotSpot específico no existe
    @ExceptionHandler(HotSpotNotFoundException::class)
    fun handleHotSpotNotFound(ex: HotSpotNotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    // 404 - Cuando una Verificación específica no existe
    @ExceptionHandler(VerificationNotFoundException::class)
    fun handleVerificationNotFound(ex: VerificationNotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    // 404 - Cuando un LocationShare no existe o ya expiró
    @ExceptionHandler(LocationShareNotFoundException::class)
    fun handleLocationShareNotFound(ex: LocationShareNotFoundException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    // 400 - Cuando el JSON enviado está mal formado o falta un campo obligatorio
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMalformedJson(ex: HttpMessageNotReadableException): ResponseEntity<String> {
        return ResponseEntity("El cuerpo de la solicitud está mal formado o incompleto", HttpStatus.BAD_REQUEST)
    }

    // 400 - Cuando los datos no pasan las validaciones
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, String?>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity(errors, HttpStatus.BAD_REQUEST)
    }

    // 400 - Cuando un parámetro de la URL tiene tipo incorrecto
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): ResponseEntity<String> {
        return ResponseEntity("El parámetro '${ex.name}' tiene un formato inválido", HttpStatus.BAD_REQUEST)
    }

    // 400 - Errores de argumentos inválidos lanzados manualmente
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.BAD_REQUEST)
    }

    // 403 - Cuando un usuario intenta modificar algo que no le pertenece
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
    fun handleAccessDenied(ex: org.springframework.security.access.AccessDeniedException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.FORBIDDEN)
    }
    // 404 - RuntimeException genérico
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    // 500 - Cualquier otra excepción no controlada
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<String> {
        return ResponseEntity("Error interno del servidor: ${ex.message}", HttpStatus.INTERNAL_SERVER_ERROR)
    }
}