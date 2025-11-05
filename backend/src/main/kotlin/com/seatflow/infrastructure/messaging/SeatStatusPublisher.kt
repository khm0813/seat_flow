package com.seatflow.infrastructure.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import com.seatflow.common.SeatStatus
import mu.KotlinLogging
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Component
class SeatStatusPublisher(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val CHANNEL_PREFIX = "seats:"
    }

    fun publishSeatStatusChange(
        showId: Long,
        seatId: String,
        status: SeatStatus,
        userId: String,
        holdExpiresAt: LocalDateTime? = null
    ): Mono<Long> {
        val channel = generateChannelName(showId)
        val message = SeatStatusMessage(
            seatId = seatId,
            status = status,
            userId = userId,
            holdExpiresAt = holdExpiresAt
        )

        return try {
            val messageJson = objectMapper.writeValueAsString(message)

            logger.debug { "Publishing seat status change to channel $channel: $messageJson" }

            redisTemplate.convertAndSend(channel, messageJson)
                .doOnSuccess { subscriberCount ->
                    logger.info { "Published seat status change for seat $seatId to $subscriberCount subscribers" }
                }
                .doOnError { error ->
                    logger.error(error) { "Failed to publish seat status change for seat $seatId" }
                }
        } catch (e: Exception) {
            logger.error(e) { "Failed to serialize seat status message for seat $seatId" }
            Mono.error(e)
        }
    }

    private fun generateChannelName(showId: Long): String {
        return "$CHANNEL_PREFIX$showId"
    }
}