package com.pucetec.securitydev.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class Users(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    val name: String = "",
    val email: String = "",
    val number: String = "",
    val password: String = "",

    @OneToMany(mappedBy = "users", cascade = [CascadeType.ALL], orphanRemoval = true)
    val hotSpots: MutableList<HotSpot> = mutableListOf()
)