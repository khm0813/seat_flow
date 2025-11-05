package com.seatflow.domain.reservation.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.seatflow.common.ReservationStatus
import com.seatflow.common.SeatStatus
import com.seatflow.domain.reservation.dto.ConfirmReservationResponse
import com.seatflow.domain.reservation.dto.HoldSeatRequest
import com.seatflow.domain.reservation.dto.HoldSeatResponse
import com.seatflow.domain.reservation.entity.Reservation
import com.seatflow.domain.reservation.repository.ReservationRepository
import com.seatflow.domain.show.repository.SeatInventoryRepository
import com.seatflow.infrastructure.idempotency.IdempotencyResult
import com.seatflow.infrastructure.idempotency.IdempotencyService
import com.seatflow.infrastructure.lock.LockResult
import com.seatflow.infrastructure.lock.RedisLockManager
import com.seatflow.infrastructure.messaging.SeatStatusPublisher
import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import kotlin.jvm.optionals.getOrNull

@Service
class ReservationService(
    private val reservationRepository: ReservationRepository,
    private val seatInventoryRepository: SeatInventoryRepository,
    private val redisLockManager: RedisLockManager,
    private val idempotencyService: IdempotencyService,
    private val seatStatusPublisher: SeatStatusPublisher,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val HOLD_DURATION_MINUTES = 10L
    }

    fun holdSeat(request: HoldSeatRequest, idempotencyKey: String): Mono<HoldSeatResponse> {
        logger.info { "Attempting to hold seat ${request.seatId} for user ${request.userId} in show ${request.showId}" }

        return idempotencyService.checkAndSetIdempotencyKey(
            idempotencyKey,
            "PROCESSING", // Temporary value while processing
            60 // 1 hour TTL
        ).flatMap { idempotencyResult ->
            when (idempotencyResult) {
                is IdempotencyResult.FirstRequest -> {
                    performSeatHold(request, idempotencyKey)
                }
                is IdempotencyResult.DuplicateRequest -> {
                    if (idempotencyResult.storedResult == "PROCESSING") {
                        Mono.error(RuntimeException("Request is still being processed"))
                    } else {
                        try {
                            val storedResponse = objectMapper.readValue(
                                idempotencyResult.storedResult,
                                HoldSeatResponse::class.java
                            )
                            logger.info { "Returning cached result for idempotency key: $idempotencyKey" }
                            Mono.just(storedResponse)
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to deserialize cached result" }
                            Mono.error(RuntimeException("Failed to process duplicate request"))
                        }
                    }
                }
                is IdempotencyResult.Error -> {
                    Mono.error(RuntimeException("Idempotency check failed: ${idempotencyResult.message}"))
                }
            }
        }
    }

    private fun performSeatHold(request: HoldSeatRequest, idempotencyKey: String): Mono<HoldSeatResponse> {
        return redisLockManager.acquireLock(request.showId, request.seatId, 300) // 5-minute lock
            .flatMap { lockResult ->
                when (lockResult) {
                    is LockResult.Success -> {
                        processSeatHoldWithLock(request, idempotencyKey, lockResult)
                            .doFinally {
                                // Always release the lock in the end
                                redisLockManager.releaseLock(lockResult.lockKey, lockResult.fencingToken)
                                    .subscribe(
                                        { released ->
                                            if (released) {
                                                logger.debug { "Successfully released lock: ${lockResult.lockKey}" }
                                            } else {
                                                logger.warn { "Failed to release lock: ${lockResult.lockKey}" }
                                            }
                                        },
                                        { error ->
                                            logger.error(error) { "Error releasing lock: ${lockResult.lockKey}" }
                                        }
                                    )
                            }
                    }
                    is LockResult.AlreadyLocked -> {
                        val errorMessage = "Seat ${request.seatId} is currently being processed by another request"
                        logger.warn { errorMessage }
                        Mono.error(RuntimeException(errorMessage))
                    }
                    is LockResult.Error -> {
                        val errorMessage = "Failed to acquire lock for seat ${request.seatId}: ${lockResult.message}"
                        logger.error { errorMessage }
                        Mono.error(RuntimeException(errorMessage))
                    }
                }
            }
    }

    @Transactional
    private fun processSeatHoldWithLock(
        request: HoldSeatRequest,
        idempotencyKey: String,
        lockResult: LockResult.Success
    ): Mono<HoldSeatResponse> {
        return seatInventoryRepository.findByShowIdAndSeatId(request.showId, request.seatId)
            .switchIfEmpty(
                Mono.error(NoSuchElementException("Seat ${request.seatId} not found in show ${request.showId}"))
            )
            .flatMap { seatInventory ->
                if (seatInventory.status != SeatStatus.AVAILABLE) {
                    Mono.error(IllegalStateException("Seat ${request.seatId} is not available (current status: ${seatInventory.status})"))
                } else {
                    val holdExpiresAt = LocalDateTime.now().plusMinutes(HOLD_DURATION_MINUTES)

                    val reservation = Reservation(
                        showId = request.showId,
                        seatInventoryId = seatInventory.id!!,
                        userId = request.userId,
                        status = ReservationStatus.HOLD,
                        holdExpiresAt = holdExpiresAt,
                        totalPrice = seatInventory.price,
                        idempotencyKey = idempotencyKey
                    )

                    // Save reservation and update seat status atomically
                    reservationRepository.save(reservation)
                        .flatMap { savedReservation ->
                            seatInventoryRepository.updateSeatStatus(
                                request.showId,
                                request.seatId,
                                SeatStatus.HOLD
                            ).then(
                                // Publish seat status change
                                seatStatusPublisher.publishSeatStatusChange(
                                    showId = request.showId,
                                    seatId = request.seatId,
                                    status = SeatStatus.HOLD,
                                    userId = request.userId,
                                    holdExpiresAt = holdExpiresAt
                                )
                            ).then(Mono.just(savedReservation))
                        }
                        .map { savedReservation ->
                            HoldSeatResponse(
                                reservationId = savedReservation.id!!,
                                showId = savedReservation.showId,
                                seatId = request.seatId,
                                userId = savedReservation.userId,
                                status = savedReservation.status,
                                holdExpiresAt = savedReservation.holdExpiresAt!!,
                                totalPrice = savedReservation.totalPrice
                            )
                        }
                        .flatMap { response ->
                            // Cache the successful response
                            val responseJson = objectMapper.writeValueAsString(response)
                            idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, responseJson)
                                .then(Mono.just(response))
                        }
                        .onErrorMap { error ->
                            when (error) {
                                is DataIntegrityViolationException -> {
                                    logger.warn { "Concurrent modification detected for seat ${request.seatId}" }
                                    RuntimeException("Seat ${request.seatId} was reserved by another user")
                                }
                                else -> {
                                    logger.error(error) { "Failed to hold seat ${request.seatId}" }
                                    RuntimeException("Failed to hold seat: ${error.message}")
                                }
                            }
                        }
                }
            }
    }

    fun confirmReservation(reservationId: Long, idempotencyKey: String): Mono<ConfirmReservationResponse> {
        logger.info { "Attempting to confirm reservation $reservationId" }

        return idempotencyService.checkAndSetIdempotencyKey(
            idempotencyKey,
            "PROCESSING",
            60
        ).flatMap { idempotencyResult ->
            when (idempotencyResult) {
                is IdempotencyResult.FirstRequest -> {
                    performReservationConfirmation(reservationId, idempotencyKey)
                }
                is IdempotencyResult.DuplicateRequest -> {
                    if (idempotencyResult.storedResult == "PROCESSING") {
                        Mono.error(RuntimeException("Request is still being processed"))
                    } else {
                        try {
                            val storedResponse = objectMapper.readValue(
                                idempotencyResult.storedResult,
                                ConfirmReservationResponse::class.java
                            )
                            logger.info { "Returning cached result for idempotency key: $idempotencyKey" }
                            Mono.just(storedResponse)
                        } catch (e: Exception) {
                            logger.error(e) { "Failed to deserialize cached result" }
                            Mono.error(RuntimeException("Failed to process duplicate request"))
                        }
                    }
                }
                is IdempotencyResult.Error -> {
                    Mono.error(RuntimeException("Idempotency check failed: ${idempotencyResult.message}"))
                }
            }
        }
    }

    @Transactional
    private fun performReservationConfirmation(
        reservationId: Long,
        idempotencyKey: String
    ): Mono<ConfirmReservationResponse> {
        return reservationRepository.findById(reservationId)
            .switchIfEmpty(Mono.error(NoSuchElementException("Reservation $reservationId not found")))
            .flatMap { reservation ->
                when {
                    reservation.status == ReservationStatus.CONFIRMED -> {
                        // Already confirmed, return the current state
                        createConfirmationResponse(reservation, idempotencyKey)
                    }
                    reservation.status != ReservationStatus.HOLD -> {
                        Mono.error(IllegalStateException("Reservation $reservationId cannot be confirmed (current status: ${reservation.status})"))
                    }
                    reservation.holdExpiresAt?.isBefore(LocalDateTime.now()) == true -> {
                        Mono.error(IllegalStateException("Reservation $reservationId has expired"))
                    }
                    else -> {
                        // Update reservation and seat status
                        reservationRepository.updateStatus(reservationId, ReservationStatus.CONFIRMED)
                            .then(
                                seatInventoryRepository.findById(reservation.seatInventoryId)
                                    .flatMap { seatInventory ->
                                        seatInventoryRepository.updateSeatStatus(
                                            reservation.showId,
                                            seatInventory.seatId,
                                            SeatStatus.CONFIRMED
                                        ).then(
                                            // Publish seat status change
                                            seatStatusPublisher.publishSeatStatusChange(
                                                showId = reservation.showId,
                                                seatId = seatInventory.seatId,
                                                status = SeatStatus.CONFIRMED,
                                                userId = reservation.userId
                                            )
                                        )
                                    }
                            )
                            .then(reservationRepository.findById(reservationId))
                            .flatMap { updatedReservation ->
                                createConfirmationResponse(updatedReservation, idempotencyKey)
                            }
                    }
                }
            }
    }

    private fun createConfirmationResponse(
        reservation: Reservation,
        idempotencyKey: String
    ): Mono<ConfirmReservationResponse> {
        return seatInventoryRepository.findById(reservation.seatInventoryId)
            .map { seatInventory ->
                ConfirmReservationResponse(
                    reservationId = reservation.id!!,
                    showId = reservation.showId,
                    seatId = seatInventory.seatId,
                    userId = reservation.userId,
                    status = reservation.status,
                    totalPrice = reservation.totalPrice,
                    confirmedAt = reservation.updatedAt
                )
            }
            .flatMap { response ->
                // Cache the successful response
                val responseJson = objectMapper.writeValueAsString(response)
                idempotencyService.checkAndSetIdempotencyKey(idempotencyKey, responseJson)
                    .then(Mono.just(response))
            }
    }
}