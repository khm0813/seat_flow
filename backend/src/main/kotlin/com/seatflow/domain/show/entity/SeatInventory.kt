package com.seatflow.domain.show.entity

import com.seatflow.common.SeatStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("seat_inventory")
data class SeatInventory(
    @Id
    val id: Long? = null,
    val showId: Long,
    val seatId: String,
    val rowName: String,
    val seatNumber: Int,
    val status: SeatStatus = SeatStatus.AVAILABLE,
    val price: BigDecimal,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)