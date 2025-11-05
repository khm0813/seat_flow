package com.seatflow.domain.reservation.dto

import com.seatflow.common.ReservationStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class HoldSeatRequest(
    val showId: Long,
    val seatId: String,
    val userId: String
)

data class HoldSeatResponse(
    val reservationId: Long,
    val showId: Long,
    val seatId: String,
    val userId: String,
    val status: ReservationStatus,
    val holdExpiresAt: LocalDateTime,
    val totalPrice: BigDecimal
)

data class ConfirmReservationResponse(
    val reservationId: Long,
    val showId: Long,
    val seatId: String,
    val userId: String,
    val status: ReservationStatus,
    val totalPrice: BigDecimal,
    val confirmedAt: LocalDateTime
)