package com.seatflow.domain.show.service

import com.seatflow.domain.show.dto.SeatDto
import com.seatflow.domain.show.dto.ShowSeatsResponse
import com.seatflow.domain.show.repository.ShowRepository
import com.seatflow.domain.show.repository.SeatInventoryRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ShowService(
    private val showRepository: ShowRepository,
    private val seatInventoryRepository: SeatInventoryRepository
) {
    private val logger = KotlinLogging.logger {}

    fun getShowSeats(showId: Long): Mono<ShowSeatsResponse> {
        logger.debug { "Getting seats for show ID: $showId" }

        return showRepository.findById(showId)
            .switchIfEmpty(Mono.error(NoSuchElementException("Show not found with ID: $showId")))
            .flatMap { show ->
                seatInventoryRepository.findByShowIdOrderByRowNameAscSeatNumberAsc(showId)
                    .map { seatInventory ->
                        SeatDto(
                            seatId = seatInventory.seatId,
                            rowName = seatInventory.rowName,
                            seatNumber = seatInventory.seatNumber,
                            status = seatInventory.status,
                            price = seatInventory.price
                        )
                    }
                    .collectList()
                    .map { seats ->
                        ShowSeatsResponse(
                            showId = show.id!!,
                            showTitle = show.title,
                            venue = show.venue,
                            showDate = show.showDate,
                            seats = seats
                        )
                    }
            }
            .doOnSuccess { response ->
                logger.debug { "Successfully retrieved ${response.seats.size} seats for show: ${response.showTitle}" }
            }
            .doOnError { error ->
                logger.error(error) { "Error retrieving seats for show ID: $showId" }
            }
    }
}