package com.seatflow.domain.show.service

import com.seatflow.common.SeatStatus
import com.seatflow.domain.show.entity.SeatInventory
import com.seatflow.domain.show.entity.Show
import com.seatflow.domain.show.repository.SeatInventoryRepository
import com.seatflow.domain.show.repository.ShowRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.time.LocalDateTime

class ShowServiceTest {

    private val showRepository = mockk<ShowRepository>()
    private val seatInventoryRepository = mockk<SeatInventoryRepository>()
    private val showService = ShowService(showRepository, seatInventoryRepository)

    @Test
    fun `should return show seats when show exists`() {
        val showId = 1L
        val show = Show(
            id = showId,
            title = "Test Concert",
            venue = "Test Venue",
            showDate = LocalDateTime.now().plusDays(1),
            totalSeats = 2
        )
        val seats = listOf(
            SeatInventory(
                id = 1L,
                showId = showId,
                seatId = "A1",
                rowName = "A",
                seatNumber = 1,
                status = SeatStatus.AVAILABLE,
                price = BigDecimal("100.00")
            ),
            SeatInventory(
                id = 2L,
                showId = showId,
                seatId = "A2",
                rowName = "A",
                seatNumber = 2,
                status = SeatStatus.HOLD,
                price = BigDecimal("100.00")
            )
        )

        every { showRepository.findById(showId) } returns Mono.just(show)
        every { seatInventoryRepository.findByShowIdOrderByRowNameAscSeatNumberAsc(showId) } returns Flux.fromIterable(seats)

        StepVerifier.create(showService.getShowSeats(showId))
            .expectNextMatches { response ->
                response.showId == showId &&
                        response.showTitle == "Test Concert" &&
                        response.seats.size == 2 &&
                        response.seats[0].seatId == "A1" &&
                        response.seats[0].status == SeatStatus.AVAILABLE &&
                        response.seats[1].seatId == "A2" &&
                        response.seats[1].status == SeatStatus.HOLD
            }
            .verifyComplete()
    }

    @Test
    fun `should return error when show does not exist`() {
        val showId = 999L

        every { showRepository.findById(showId) } returns Mono.empty()

        StepVerifier.create(showService.getShowSeats(showId))
            .expectErrorMatches { error ->
                error is NoSuchElementException && error.message?.contains("Show not found") == true
            }
            .verify()
    }
}