package com.seatflow.domain.show.repository

import com.seatflow.domain.show.entity.Show
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ShowRepository : ReactiveCrudRepository<Show, Long>