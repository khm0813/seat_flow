package com.seatflow

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SeatFlowApplication

fun main(args: Array<String>) {
    runApplication<SeatFlowApplication>(*args)
}