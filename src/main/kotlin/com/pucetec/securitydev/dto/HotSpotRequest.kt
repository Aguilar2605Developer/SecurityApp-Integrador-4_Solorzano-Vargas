// HotSpotRequest.kt
package com.pucetec.securitydev.dto

data class HotSpotRequest(
    val latitude: Double,
    val longitude: Double,
    val modality: String = "",
    val description: String = "",
    val userId: Long
)