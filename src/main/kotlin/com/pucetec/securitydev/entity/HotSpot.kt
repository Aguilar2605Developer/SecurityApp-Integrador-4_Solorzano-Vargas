package com.pucetec.securitydev.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

//Con Entity indicamos a Kotlin que esto es una tabla en la base de datos.
@Entity
@Table(name = "hotspot")
class HotSpot(
    @Id
    //Definimos un Id autoincremental
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    val latitude: Double=0.0,
    val longitude: Double=0.0,
    val modality: String="",
    val description: String="",

    @ManyToOne
    @JoinColumn(name = "user_id") // Esta anotación le dice a la base de datos cómo se llamará la columna de unión (FK)
    // El signo de pregunta nos dice que el valor puede llegar vacio
    val users: Users? = null
)