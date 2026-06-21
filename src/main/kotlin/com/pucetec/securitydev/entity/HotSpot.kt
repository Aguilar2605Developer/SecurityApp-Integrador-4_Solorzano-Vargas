package com.pucetec.securitydev.entity

import jakarta.persistence.*

@Entity
@Table(name = "hotspot")
class HotSpot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val modality: String = "",
    val description: String = "",

    @ManyToOne(fetch = FetchType.LAZY) // Lazy evita errores de carga recursiva
    @JoinColumn(name = "user_id", nullable = true) // nullable = true permite que el hotspot sea sin usuario
    val users: Users? = null
)