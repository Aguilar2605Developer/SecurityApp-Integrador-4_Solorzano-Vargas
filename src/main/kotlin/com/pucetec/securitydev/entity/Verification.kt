package com.pucetec.securitydev.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "verification")
open class Verification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open val id: Long = 0L,

    @Column(name = "created_at")
    open val createdAt: LocalDateTime = LocalDateTime.now(),

    open val status: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    open val user: Users,

    @ManyToOne(fetch = FetchType.LAZY)
    open val hotSpot: HotSpot,
)