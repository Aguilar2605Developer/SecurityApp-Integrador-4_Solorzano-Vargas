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
// Uso open class para permitir que Spring pueda crear proxies de esta clase
open class Verification(
    // Este campo es la clave primaria
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
   val id: Long=0L,
    // Guardo la fecha y hora exacta en que se creó el verification
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val status: String = "",

    // Muchass alertas pueden llegar a un Usuario
    @ManyToOne(fetch = FetchType.LAZY)
    val user: Users,

    //Carga perezosa (LAZY): el punto caliente solo se carga si la app lo solicita de forma explícita
    @ManyToOne(fetch = FetchType.LAZY)
    val hotSpot: HotSpot,
)