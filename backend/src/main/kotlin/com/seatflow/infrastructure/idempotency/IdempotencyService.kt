package com.seatflow.infrastructure.idempotency

import mu.KotlinLogging
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class IdempotencyService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val IDEMPOTENCY_PREFIX = "idempotency:"
        private const val DEFAULT_TTL_MINUTES = 60L
    }

    fun checkAndSetIdempotencyKey(
        idempotencyKey: String,
        result: String,
        ttlMinutes: Long = DEFAULT_TTL_MINUTES
    ): Mono<IdempotencyResult> {
        val key = generateIdempotencyKey(idempotencyKey)

        logger.debug { "Checking idempotency key: $key" }

        return redisTemplate.opsForValue()
            .setIfAbsent(key, result, Duration.ofMinutes(ttlMinutes))
            .flatMap { wasSet ->
                if (wasSet == true) {
                    logger.debug { "New idempotency key set: $key" }
                    Mono.just(IdempotencyResult.FirstRequest(key))
                } else {
                    // Key already exists, get the stored result
                    redisTemplate.opsForValue()
                        .get(key)
                        .map { storedResult ->
                            logger.debug { "Duplicate request detected for key: $key" }
                            IdempotencyResult.DuplicateRequest(key, storedResult)
                        }
                        .switchIfEmpty(
                            Mono.just(IdempotencyResult.Error(key, "Failed to retrieve stored result"))
                        )
                }
            }
            .onErrorReturn { error ->
                logger.error(error) { "Error checking idempotency key: $key" }
                IdempotencyResult.Error(key, error.message ?: "Unknown error")
            }
    }

    fun getStoredResult(idempotencyKey: String): Mono<String?> {
        val key = generateIdempotencyKey(idempotencyKey)
        return redisTemplate.opsForValue()
            .get(key)
            .onErrorReturn(null)
    }

    fun removeIdempotencyKey(idempotencyKey: String): Mono<Boolean> {
        val key = generateIdempotencyKey(idempotencyKey)
        return redisTemplate.delete(key)
            .map { deletedCount -> deletedCount > 0 }
            .onErrorReturn(false)
    }

    private fun generateIdempotencyKey(idempotencyKey: String): String {
        return "$IDEMPOTENCY_PREFIX$idempotencyKey"
    }
}

sealed class IdempotencyResult {
    abstract val key: String

    data class FirstRequest(override val key: String) : IdempotencyResult()

    data class DuplicateRequest(
        override val key: String,
        val storedResult: String
    ) : IdempotencyResult()

    data class Error(
        override val key: String,
        val message: String
    ) : IdempotencyResult()
}