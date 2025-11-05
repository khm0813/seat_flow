package com.seatflow.infrastructure.messaging

import com.seatflow.common.SeatStatus
import java.time.LocalDateTime

data class SeatStatusMessage(
    val seatId: String,
    val status: SeatStatus,
    val userId: String,
    val holdExpiresAt: LocalDateTime? = null
)