// HotSpotResponse.kt
package com.pucetec.securitydev.dto

data class HotSpotResponse(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val modality: String,
    val description: String,
    val userId: Long?,
    val username: String?
)