package com.pucetec.securitydev.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class Users(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(unique = true, nullable = false)
    val cognitoSub: String = "",

    val name: String = "",
    val email: String = "",
    val number: String = "",

    @OneToMany(mappedBy = "users", cascade = [CascadeType.ALL], orphanRemoval = true)
    val hotSpots: MutableList<HotSpot> = mutableListOf()
)