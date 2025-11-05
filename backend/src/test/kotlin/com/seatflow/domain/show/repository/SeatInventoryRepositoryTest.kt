package com.seatflow.domain.show.repository

import com.seatflow.common.SeatStatus
import com.seatflow.domain.show.entity.SeatInventory
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.test.context.TestConstructor
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.time.LocalDateTime

@DataR2dbcTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SeatInventoryRepositoryTest(
    private val seatInventoryRepository: SeatInventoryRepository
) {

    @Test
    fun `should find seats by show id ordered by row and seat number`() {
        val showId = 1L
        val seat1 = SeatInventory(
            showId = showId,
            seatId = "A1",
            rowName = "A",
            seatNumber = 1,
            status = SeatStatus.AVAILABLE,
            price = BigDecimal("100.00")
        )
        val seat2 = SeatInventory(
            showId = showId,
            seatId = "B2",
            rowName = "B",
            seatNumber = 2,
            status = SeatStatus.AVAILABLE,
            price = BigDecimal("80.00")
        )

        seatInventoryRepository.save(seat1)
            .then(seatInventoryRepository.save(seat2))
            .then(seatInventoryRepository.findByShowIdOrderByRowNameAscSeatNumberAsc(showId).collectList())
            .`as` { StepVerifier.create(it) }
            .expectNextMatches { seats ->
                seats.size == 2 &&
                        seats[0].seatId == "A1" &&
                        seats[1].seatId == "B2"
            }
            .verifyComplete()
    }

    @Test
    fun `should find seat by show id and seat id`() {
        val showId = 1L
        val seatId = "A1"
        val seat = SeatInventory(
            showId = showId,
            seatId = seatId,
            rowName = "A",
            seatNumber = 1,
            status = SeatStatus.AVAILABLE,
            price = BigDecimal("100.00")
        )

        seatInventoryRepository.save(seat)
            .then(seatInventoryRepository.findByShowIdAndSeatId(showId, seatId))
            .`as` { StepVerifier.create(it) }
            .expectNextMatches { foundSeat ->
                foundSeat.seatId == seatId && foundSeat.showId == showId
            }
            .verifyComplete()
    }
}