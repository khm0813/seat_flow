package com.seatflow.domain.reservation.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seatflow.common.ReservationStatus
import com.seatflow.common.SeatStatus
import com.seatflow.domain.reservation.dto.HoldSeatRequest
import com.seatflow.domain.reservation.entity.Reservation
import com.seatflow.domain.reservation.repository.ReservationRepository
import com.seatflow.domain.show.entity.SeatInventory
import com.seatflow.domain.show.repository.SeatInventoryRepository
import com.seatflow.infrastructure.idempotency.IdempotencyResult
import com.seatflow.infrastructure.idempotency.IdempotencyService
import com.seatflow.infrastructure.lock.LockResult
import com.seatflow.infrastructure.lock.RedisLockManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.time.LocalDateTime

class ReservationServiceTest {

    private val reservationRepository = mockk<ReservationRepository>()
    private val seatInventoryRepository = mockk<SeatInventoryRepository>()
    private val redisLockManager = mockk<RedisLockManager>()
    private val idempotencyService = mockk<IdempotencyService>()
    private val seatStatusPublisher = mockk<com.seatflow.infrastructure.messaging.SeatStatusPublisher>(relaxed = true)
    private val objectMapper = ObjectMapper()

    private val reservationService = ReservationService(
        reservationRepository,
        seatInventoryRepository,
        redisLockManager,
        idempotencyService,
        seatStatusPublisher,
        objectMapper
    )

    @Test
    fun `should hold seat successfully when all conditions are met`() {
        val request = HoldSeatRequest(1L, "A1", "user123")
        val idempotencyKey = "test-key"
        val lockResult = LockResult.Success("lock:seat:1:A1", "token123", 300)
        val seatInventory = SeatInventory(
            id = 1L,
            showId = 1L,
            seatId = "A1",
            rowName = "A",
            seatNumber = 1,
            status = SeatStatus.AVAILABLE,
            price = BigDecimal("100.00")
        )
        val savedReservation = Reservation(
            id = 1L,
            showId = 1L,
            seatInventoryId = 1L,
            userId = "user123",
            status = ReservationStatus.HOLD,
            holdExpiresAt = LocalDateTime.now().plusMinutes(10),
            totalPrice = BigDecimal("100.00"),
            idempotencyKey = idempotencyKey
        )

        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "PROCESSING", 60) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))
        every { redisLockManager.acquireLock(1L, "A1", 300) } returns Mono.just(lockResult)
        every { seatInventoryRepository.findByShowIdAndSeatId(1L, "A1") } returns Mono.just(seatInventory)
        every { reservationRepository.save(any()) } returns Mono.just(savedReservation)
        every { seatInventoryRepository.updateSeatStatus(1L, "A1", SeatStatus.HOLD) } returns Mono.just(1)
        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, any()) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))
        every { redisLockManager.releaseLock("lock:seat:1:A1", "token123") } returns Mono.just(true)

        StepVerifier.create(reservationService.holdSeat(request, idempotencyKey))
            .expectNextMatches { response ->
                response.reservationId == 1L &&
                        response.seatId == "A1" &&
                        response.userId == "user123" &&
                        response.status == ReservationStatus.HOLD
            }
            .verifyComplete()

        verify { redisLockManager.acquireLock(1L, "A1", 300) }
        verify { seatInventoryRepository.findByShowIdAndSeatId(1L, "A1") }
        verify { reservationRepository.save(any()) }
        verify { seatInventoryRepository.updateSeatStatus(1L, "A1", SeatStatus.HOLD) }
    }

    @Test
    fun `should return error when seat is not available`() {
        val request = HoldSeatRequest(1L, "A1", "user123")
        val idempotencyKey = "test-key"
        val lockResult = LockResult.Success("lock:seat:1:A1", "token123", 300)
        val seatInventory = SeatInventory(
            id = 1L,
            showId = 1L,
            seatId = "A1",
            rowName = "A",
            seatNumber = 1,
            status = SeatStatus.CONFIRMED, // Not available
            price = BigDecimal("100.00")
        )

        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "PROCESSING", 60) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))
        every { redisLockManager.acquireLock(1L, "A1", 300) } returns Mono.just(lockResult)
        every { seatInventoryRepository.findByShowIdAndSeatId(1L, "A1") } returns Mono.just(seatInventory)
        every { redisLockManager.releaseLock("lock:seat:1:A1", "token123") } returns Mono.just(true)

        StepVerifier.create(reservationService.holdSeat(request, idempotencyKey))
            .expectErrorMatches { error ->
                error is IllegalStateException && error.message?.contains("not available") == true
            }
            .verify()
    }

    @Test
    fun `should return error when lock cannot be acquired`() {
        val request = HoldSeatRequest(1L, "A1", "user123")
        val idempotencyKey = "test-key"

        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "PROCESSING", 60) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))
        every { redisLockManager.acquireLock(1L, "A1", 300) } returns
                Mono.just(LockResult.AlreadyLocked("lock:seat:1:A1"))

        StepVerifier.create(reservationService.holdSeat(request, idempotencyKey))
            .expectErrorMatches { error ->
                error is RuntimeException && error.message?.contains("being processed") == true
            }
            .verify()
    }

    @Test
    fun `should return cached result for duplicate request`() {
        val request = HoldSeatRequest(1L, "A1", "user123")
        val idempotencyKey = "test-key"
        val cachedResponse = """{"reservationId":1,"showId":1,"seatId":"A1","userId":"user123","status":"HOLD","holdExpiresAt":"2023-01-01T10:00:00","totalPrice":100.00}"""

        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "PROCESSING", 60) } returns
                Mono.just(IdempotencyResult.DuplicateRequest("key", cachedResponse))

        StepVerifier.create(reservationService.holdSeat(request, idempotencyKey))
            .expectNextMatches { response ->
                response.reservationId == 1L && response.seatId == "A1"
            }
            .verifyComplete()
    }

    @Test
    fun `should confirm reservation successfully`() {
        val reservationId = 1L
        val idempotencyKey = "confirm-key"
        val reservation = Reservation(
            id = reservationId,
            showId = 1L,
            seatInventoryId = 1L,
            userId = "user123",
            status = ReservationStatus.HOLD,
            holdExpiresAt = LocalDateTime.now().plusMinutes(5),
            totalPrice = BigDecimal("100.00")
        )
        val seatInventory = SeatInventory(
            id = 1L,
            showId = 1L,
            seatId = "A1",
            rowName = "A",
            seatNumber = 1,
            status = SeatStatus.HOLD,
            price = BigDecimal("100.00")
        )
        val confirmedReservation = reservation.copy(status = ReservationStatus.CONFIRMED)

        // Initial idempotency check
        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "PROCESSING", 60) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))

        // First findById - get the reservation
        every { reservationRepository.findById(reservationId) } returnsMany listOf(
            Mono.just(reservation),
            Mono.just(confirmedReservation)  // Second call after update
        )

        // Update reservation status
        every { reservationRepository.updateStatus(reservationId, ReservationStatus.CONFIRMED) } returns Mono.just(1)

        // FindById called twice: once for update, once for response
        every { seatInventoryRepository.findById(1L) } returns Mono.just(seatInventory)

        // Update seat status
        every { seatInventoryRepository.updateSeatStatus(1L, "A1", SeatStatus.CONFIRMED) } returns Mono.just(1)

        // Final idempotency cache update with JSON response
        every { idempotencyService.checkAndSetIdempotencyKey(eq(idempotencyKey), any()) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))

        StepVerifier.create(reservationService.confirmReservation(reservationId, idempotencyKey))
            .expectNextMatches { response ->
                response.reservationId == reservationId &&
                        response.status == ReservationStatus.CONFIRMED &&
                        response.seatId == "A1"
            }
            .verifyComplete()
    }

    @Test
    fun `should return error when trying to confirm expired reservation`() {
        val reservationId = 1L
        val idempotencyKey = "confirm-key"
        val expiredReservation = Reservation(
            id = reservationId,
            showId = 1L,
            seatInventoryId = 1L,
            userId = "user123",
            status = ReservationStatus.HOLD,
            holdExpiresAt = LocalDateTime.now().minusMinutes(1), // Expired
            totalPrice = BigDecimal("100.00")
        )

        every { idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, "PROCESSING", 60) } returns
                Mono.just(IdempotencyResult.FirstRequest("key"))
        every { reservationRepository.findById(reservationId) } returns Mono.just(expiredReservation)

        StepVerifier.create(reservationService.confirmReservation(reservationId, idempotencyKey))
            .expectErrorMatches { error ->
                error is IllegalStateException && error.message?.contains("expired") == true
            }
            .verify()
    }
}