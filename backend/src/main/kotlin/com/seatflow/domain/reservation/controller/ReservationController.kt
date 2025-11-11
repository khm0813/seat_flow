package com.seatflow.domain.reservation.controller

import com.seatflow.common.ApiResponse
import com.seatflow.domain.reservation.dto.ConfirmReservationResponse
import com.seatflow.domain.reservation.dto.HoldSeatRequest
import com.seatflow.domain.reservation.dto.HoldSeatResponse
import com.seatflow.domain.reservation.service.ReservationService
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:3000"])
class ReservationController(
    private val reservationService: ReservationService
) {
    private val logger = KotlinLogging.logger {}

    @PostMapping("/hold")
    fun holdSeat(
        @Valid @RequestBody request: HoldSeatRequest,
        @RequestHeader("Idempotency-Key") idempotencyKey: String
    ): Mono<ApiResponse<HoldSeatResponse>> {
        logger.info { "Received hold seat request: $request with idempotency key: $idempotencyKey" }

        return reservationService.holdSeat(request, idempotencyKey)
            .map { response ->
                logger.info { "Successfully held seat ${request.seatId} for user ${request.userId}" }
                ApiResponse.success(response)
            }
            .onErrorResume { error ->
                logger.error(error as Throwable) { "Failed to hold seat ${request.seatId} for user ${request.userId}" }
                val response: ApiResponse<HoldSeatResponse> = when (error) {
                    is NoSuchElementException -> ApiResponse.error("Seat not found")
                    is IllegalStateException -> ApiResponse.error(error.message ?: "Seat not available")
                    is RuntimeException -> ApiResponse.error(error.message ?: "Failed to hold seat")
                    else -> ApiResponse.error("Internal server error")
                }
                Mono.just(response)
            }
    }

    @PostMapping("/{reservationId}/confirm")
    fun confirmReservation(
        @PathVariable reservationId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String
    ): Mono<ApiResponse<ConfirmReservationResponse>> {
        logger.info { "Received confirm reservation request: reservationId=$reservationId with idempotency key: $idempotencyKey" }

        return reservationService.confirmReservation(reservationId, idempotencyKey)
            .map { response ->
                logger.info { "Successfully confirmed reservation $reservationId" }
                ApiResponse.success(response)
            }
            .onErrorResume { error ->
                logger.error(error as Throwable) { "Failed to confirm reservation $reservationId" }
                val response: ApiResponse<ConfirmReservationResponse> = when (error) {
                    is NoSuchElementException -> ApiResponse.error("Reservation not found")
                    is IllegalStateException -> ApiResponse.error(error.message ?: "Cannot confirm reservation")
                    is RuntimeException -> ApiResponse.error(error.message ?: "Failed to confirm reservation")
                    else -> ApiResponse.error("Internal server error")
                }
                Mono.just(response)
            }
    }
}