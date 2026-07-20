package com.pucetec.securitydev.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "location_share_recipient",
    uniqueConstraints = [UniqueConstraint(columnNames = ["location_share_id", "email"])]
)
class LocationShareRecipient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_share_id", nullable = false)
    val locationShare: LocationShare? = null,

    // Siempre normalizado en minusculas antes de guardar (ver LocationShareController)
    @Column(nullable = false)
    val email: String = "",

    val addedAt: LocalDateTime = LocalDateTime.now()
)