package com.seatflow.config

import com.seatflow.infrastructure.messaging.SeatStatusWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@Configuration
class WebSocketConfig {

    @Bean
    fun webSocketHandlerMapping(seatStatusWebSocketHandler: SeatStatusWebSocketHandler): HandlerMapping {
        val map = mapOf<String, WebSocketHandler>(
            "/ws/seats/{showId}" to seatStatusWebSocketHandler
        )

        val handlerMapping = SimpleUrlHandlerMapping()
        handlerMapping.urlMap = map
        handlerMapping.order = 1
        return handlerMapping
    }

    @Bean
    fun webSocketHandlerAdapter(): WebSocketHandlerAdapter {
        return WebSocketHandlerAdapter()
    }
}