package com.pucetec.securitydev.entity

import jakarta.persistence.*

@Entity
@Table(name = "hotspot_report")
data class HotSpotReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    val modality: String = "",
    val description: String = "",
    val peopleInvolved: Int = 1,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotspot_id", nullable = false)
    val hotSpot: HotSpot? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    val users: Users? = null
)