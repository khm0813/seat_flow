package com.seatflow.domain.reservation.repository

import com.seatflow.common.ReservationStatus
import com.seatflow.domain.reservation.entity.Reservation
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Repository
interface ReservationRepository : ReactiveCrudRepository<Reservation, Long> {

    fun findByIdempotencyKey(idempotencyKey: String): Mono<Reservation>

    fun findByShowIdAndUserId(showId: Long, userId: String): Flux<Reservation>

    @Query("SELECT * FROM reservations WHERE status = :status AND hold_expires_at < :now")
    fun findExpiredHolds(status: ReservationStatus, now: LocalDateTime): Flux<Reservation>

    @Query("UPDATE reservations SET status = :status, updated_at = NOW() WHERE id = :id")
    fun updateStatus(id: Long, status: ReservationStatus): Mono<Int>
}