package com.seatflow.domain.reservation.entity

import com.seatflow.common.ReservationStatus
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("reservations")
data class Reservation(
    @Id
    val id: Long? = null,
    val showId: Long,
    val seatInventoryId: Long,
    val userId: String,
    val status: ReservationStatus = ReservationStatus.HOLD,
    val holdExpiresAt: LocalDateTime? = null,
    val totalPrice: BigDecimal,
    val idempotencyKey: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)