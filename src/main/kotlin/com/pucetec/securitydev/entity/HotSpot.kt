package com.pucetec.securitydev.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "hotspot")
data class HotSpot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val modality: String = "",
    val description: String = "",
    val peopleInvolved: Int = 1,
    val active: Boolean = true,
    val expiresAt: LocalDateTime = LocalDateTime.now().plusHours(24),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    val users: Users? = null
)