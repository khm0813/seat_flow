package com.seatflow.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class SeatStatusWebSocketHandler(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val listenerContainer: ReactiveRedisMessageListenerContainer,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    private val logger = KotlinLogging.logger {}
    private val sessions = ConcurrentHashMap<String, Sinks.Many<String>>()

    companion object {
        private const val CHANNEL_PREFIX = "seats:"
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        val showId = extractShowIdFromPath(session.handshakeInfo.uri.path)
        if (showId == null) {
            logger.warn { "Invalid WebSocket path: ${session.handshakeInfo.uri.path}" }
            return session.close()
        }

        val sessionId = session.id
        val sink = Sinks.many().multicast().onBackpressureBuffer<String>()
        sessions[sessionId] = sink

        logger.info { "WebSocket session $sessionId connected for show $showId" }

        // Subscribe to Redis channel for this show
        val channel = "$CHANNEL_PREFIX$showId"
        val subscription = subscribeToChannel(channel, sink)

        // Send messages from sink to WebSocket client
        val output = session.send(
            sink.asFlux()
                .map { message -> session.textMessage(message) }
                .doOnError { error ->
                    logger.error(error) { "Error sending message to WebSocket session $sessionId" }
                }
        )

        // Handle incoming messages from client (optional - for ping/pong or other client messages)
        val input = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .doOnNext { message ->
                logger.debug { "Received message from WebSocket session $sessionId: $message" }
                // Handle client messages if needed (e.g., ping/pong)
            }
            .then()

        // Cleanup when session closes
        return Mono.zip(output, input)
            .doFinally {
                logger.info { "WebSocket session $sessionId disconnected" }
                sessions.remove(sessionId)
                subscription.dispose()
                sink.tryEmitComplete()
            }
            .then()
    }

    private fun subscribeToChannel(channel: String, sink: Sinks.Many<String>): reactor.core.Disposable {
        return listenerContainer.receive(PatternTopic(channel))
            .map { message -> message.message }
            .doOnNext { message ->
                logger.debug { "Received Redis message on channel $channel: $message" }
                sink.tryEmitNext(message)
            }
            .doOnError { error ->
                logger.error(error) { "Error receiving Redis message on channel $channel" }
            }
            .onErrorContinue { error, _ ->
                logger.error(error) { "Continuing after error in Redis subscription for channel $channel" }
            }
            .subscribe()
    }

    private fun extractShowIdFromPath(path: String): Long? {
        return try {
            // Expected path: /ws/seats/{showId}
            val pathParts = path.split("/")
            if (pathParts.size >= 4 && pathParts[1] == "ws" && pathParts[2] == "seats") {
                pathParts[3].toLongOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to extract showId from path: $path" }
            null
        }
    }

    // Method to get active session count for monitoring
    fun getActiveSessionCount(): Int = sessions.size

    // Method to broadcast a message to all sessions (if needed for testing)
    fun broadcastToAllSessions(message: String) {
        sessions.values.forEach { sink ->
            sink.tryEmitNext(message)
        }
    }
}