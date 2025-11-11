package com.seatflow.domain.show.controller

import com.seatflow.common.ApiResponse
import com.seatflow.domain.show.dto.ShowSeatsResponse
import com.seatflow.domain.show.service.ShowService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/shows")
@CrossOrigin(origins = ["http://localhost:5173", "http://localhost:3000"])
class ShowController(
    private val showService: ShowService
) {
    private val logger = KotlinLogging.logger {}

    @GetMapping("/{id}/seats")
    fun getShowSeats(@PathVariable id: Long): Mono<ApiResponse<ShowSeatsResponse>> {
        logger.info { "Received request for show seats: showId=$id" }

        return showService.getShowSeats(id)
            .map { response ->
                ApiResponse.success(response)
            }
            .onErrorResume { error ->
                logger.error(error as Throwable) { "Failed to get show seats for ID: $id" }
                val response: ApiResponse<ShowSeatsResponse> = when (error) {
                    is NoSuchElementException -> ApiResponse.error("Show not found")
                    else -> ApiResponse.error("Internal server error")
                }
                Mono.just(response)
            }
            .doOnSuccess { response ->
                if (response.success) {
                    logger.info { "Successfully returned seats for show: ${response.data?.showTitle}" }
                }
            }
    }
}