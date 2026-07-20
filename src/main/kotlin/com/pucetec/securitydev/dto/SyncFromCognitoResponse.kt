package com.pucetec.securitydev.dto

data class SyncFromCognitoResponse(
    val totalEnCognito: Int,
    val creados: List<String>,
    val yaExistian: Int,
    val omitidosNoConfirmados: List<String>
)