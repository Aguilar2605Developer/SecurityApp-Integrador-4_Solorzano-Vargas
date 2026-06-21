package com.pucetec.securitydev.repository

import com.pucetec.securitydev.entity.HotSpot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface HotSpotRepository : JpaRepository<HotSpot, Long> {

}