package com.pucetec.securitydev.exceptions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// Le digo a Spring que esta clase maneja las excepciones de todos los controladores
@RestControllerAdvice
class GlobalExceptionHandler {

    // Capturo los errores de lógica de negocio (como cuando no encuentra un ID en el Service) y retorno un error 404
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.NOT_FOUND)
    }

    // Capturo cualquier otra excepción no controlada y retorno un error 500
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}