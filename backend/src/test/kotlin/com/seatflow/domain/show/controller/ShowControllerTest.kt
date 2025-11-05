package com.seatflow.domain.show.controller

import com.seatflow.common.SeatStatus
import com.seatflow.domain.show.dto.SeatDto
import com.seatflow.domain.show.dto.ShowSeatsResponse
import com.seatflow.domain.show.service.ShowService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDateTime

@WebFluxTest(ShowController::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ShowControllerTest(
    private val webTestClient: WebTestClient
) {

    @MockkBean
    private lateinit var showService: ShowService

    @Test
    fun `should return show seats successfully`() {
        val showId = 1L
        val response = ShowSeatsResponse(
            showId = showId,
            showTitle = "Test Concert",
            venue = "Test Venue",
            showDate = LocalDateTime.of(2025, 12, 25, 19, 30),
            seats = listOf(
                SeatDto(
                    seatId = "A1",
                    rowName = "A",
                    seatNumber = 1,
                    status = SeatStatus.AVAILABLE,
                    price = BigDecimal("100.00")
                )
            )
        )

        every { showService.getShowSeats(showId) } returns Mono.just(response)

        webTestClient.get()
            .uri("/api/shows/$showId/seats")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(true)
            .jsonPath("$.data.showId").isEqualTo(showId)
            .jsonPath("$.data.showTitle").isEqualTo("Test Concert")
            .jsonPath("$.data.seats[0].seatId").isEqualTo("A1")
            .jsonPath("$.data.seats[0].status").isEqualTo("AVAILABLE")
    }

    @Test
    fun `should return error when show not found`() {
        val showId = 999L

        every { showService.getShowSeats(showId) } returns Mono.error(NoSuchElementException("Show not found"))

        webTestClient.get()
            .uri("/api/shows/$showId/seats")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.success").isEqualTo(false)
            .jsonPath("$.error").isEqualTo("Show not found")
    }
}