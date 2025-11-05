package com.seatflow.domain.show.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Table("shows")
data class Show(
    @Id
    val id: Long? = null,
    val title: String,
    val venue: String,
    val showDate: LocalDateTime,
    val totalSeats: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)