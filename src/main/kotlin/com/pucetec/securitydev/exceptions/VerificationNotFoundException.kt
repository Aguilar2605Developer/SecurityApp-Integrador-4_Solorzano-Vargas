package com.pucetec.securitydev.exceptions

// Excepción para cuando una alerta o verificación no existe en el sistema
class VerificationNotFoundException(message: String) : RuntimeException(message)