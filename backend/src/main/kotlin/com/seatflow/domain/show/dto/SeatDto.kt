package com.seatflow.domain.show.dto

import com.seatflow.common.SeatStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class SeatDto(
    val seatId: String,
    val rowName: String,
    val seatNumber: Int,
    val status: SeatStatus,
    val price: BigDecimal,
    val holdExpiresAt: LocalDateTime? = null
)

data class ShowSeatsResponse(
    val showId: Long,
    val showTitle: String,
    val venue: String,
    val showDate: LocalDateTime,
    val seats: List<SeatDto>
)