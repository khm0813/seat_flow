package com.seatflow.domain.show.repository

import com.seatflow.common.SeatStatus
import com.seatflow.domain.show.entity.SeatInventory
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface SeatInventoryRepository : ReactiveCrudRepository<SeatInventory, Long> {

    fun findByShowIdOrderByRowNameAscSeatNumberAsc(showId: Long): Flux<SeatInventory>

    fun findByShowIdAndSeatId(showId: Long, seatId: String): Mono<SeatInventory>

    @Query("UPDATE seat_inventory SET status = :status, updated_at = NOW() WHERE show_id = :showId AND seat_id = :seatId")
    fun updateSeatStatus(showId: Long, seatId: String, status: SeatStatus): Mono<Int>
}