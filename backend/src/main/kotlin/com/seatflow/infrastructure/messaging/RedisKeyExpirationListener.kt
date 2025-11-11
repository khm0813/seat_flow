package com.seatflow.infrastructure.messaging

import com.seatflow.common.SeatStatus
import com.seatflow.domain.show.repository.SeatInventoryRepository
import mu.KotlinLogging
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.Disposable
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy

@Component
class RedisKeyExpirationListener(
    private val redisConnectionFactory: ReactiveRedisConnectionFactory,
    private val seatInventoryRepository: SeatInventoryRepository,
    private val seatStatusPublisher: SeatStatusPublisher
) {
    private val logger = KotlinLogging.logger {}
    private lateinit var listenerContainer: ReactiveRedisMessageListenerContainer
    private var subscription: Disposable? = null

    companion object {
        private const val LOCK_PREFIX = "lock:seat:"
        private val EXPIRATION_PATTERN = PatternTopic("__keyevent@*__:expired")
    }

    @PostConstruct
    fun initialize() {
        listenerContainer = ReactiveRedisMessageListenerContainer(redisConnectionFactory)

        // Listen for key expiration events
        val messages = listenerContainer.receive(EXPIRATION_PATTERN)

        subscription = messages
            .filter { message ->
                val key = message.message
                key.startsWith(LOCK_PREFIX)
            }
            .flatMap { message ->
                val expiredKey = message.message
                handleLockExpiration(expiredKey)
                    .onErrorContinue { error, _ ->
                        logger.error(error) { "Error handling lock expiration for key: $expiredKey" }
                    }
            }
            .subscribe()

        logger.info { "Redis key expiration listener started" }
    }

    @PreDestroy
    fun destroy() {
        subscription?.dispose()
        logger.info { "Redis key expiration listener stopped" }
    }

    private fun handleLockExpiration(expiredKey: String): Mono<Void> {
        logger.info { "Handling lock expiration for key: $expiredKey" }

        return try {
            val (showId, seatId) = parseLockKey(expiredKey)

            // Find the seat and update its status to AVAILABLE
            seatInventoryRepository.findByShowIdAndSeatId(showId, seatId)
                .flatMap { seatInventory ->
                    if (seatInventory.status == SeatStatus.HOLD) {
                        logger.info { "Releasing expired hold for seat $seatId in show $showId" }

                        seatInventoryRepository.updateSeatStatus(showId, seatId, SeatStatus.AVAILABLE)
                            .then(
                                seatStatusPublisher.publishSeatStatusChange(
                                    showId = showId,
                                    seatId = seatId,
                                    status = SeatStatus.AVAILABLE,
                                    userId = "system" // System released due to expiration
                                )
                            )
                            .then()
                    } else {
                        logger.debug { "Seat $seatId status is ${seatInventory.status}, no action needed" }
                        Mono.empty()
                    }
                }
                .switchIfEmpty(
                    Mono.fromRunnable {
                        logger.warn { "Seat $seatId not found in show $showId for expired lock" }
                    }
                )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse expired lock key: $expiredKey" }
            Mono.empty()
        }
    }

    private fun parseLockKey(lockKey: String): Pair<Long, String> {
        // Expected format: "lock:seat:showId:seatId"
        val parts = lockKey.removePrefix(LOCK_PREFIX).split(":")
        if (parts.size != 2) {
            throw IllegalArgumentException("Invalid lock key format: $lockKey")
        }

        val showId = parts[0].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid showId in lock key: $lockKey")
        val seatId = parts[1]

        return Pair(showId, seatId)
    }
}