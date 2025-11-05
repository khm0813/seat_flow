package com.seatflow.infrastructure.lock

import mu.KotlinLogging
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Component
class RedisLockManager(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val LOCK_PREFIX = "lock:seat:"
        private const val DEFAULT_LOCK_TTL_SECONDS = 300L // 5 minutes

        // Lua script for atomic lock release with fencing token verification
        private val RELEASE_LOCK_SCRIPT = """
            if redis.call("GET", KEYS[1]) == ARGV[1] then
                return redis.call("DEL", KEYS[1])
            else
                return 0
            end
        """.trimIndent()
    }

    private val releaseLockScript = RedisScript.of(RELEASE_LOCK_SCRIPT, Long::class.java)

    fun acquireLock(showId: Long, seatId: String, ttlSeconds: Long = DEFAULT_LOCK_TTL_SECONDS): Mono<LockResult> {
        val lockKey = generateLockKey(showId, seatId)
        val fencingToken = generateFencingToken()

        logger.debug { "Attempting to acquire lock for key: $lockKey with token: $fencingToken" }

        return redisTemplate.opsForValue()
            .setIfAbsent(lockKey, fencingToken, Duration.ofSeconds(ttlSeconds))
            .map { acquired ->
                if (acquired == true) {
                    logger.debug { "Successfully acquired lock for key: $lockKey" }
                    LockResult.Success(lockKey, fencingToken, ttlSeconds)
                } else {
                    logger.debug { "Failed to acquire lock for key: $lockKey (already exists)" }
                    LockResult.AlreadyLocked(lockKey)
                }
            }
            .onErrorReturn { error ->
                logger.error(error) { "Error acquiring lock for key: $lockKey" }
                LockResult.Error(lockKey, error.message ?: "Unknown error")
            }
    }

    fun releaseLock(lockKey: String, fencingToken: String): Mono<Boolean> {
        logger.debug { "Attempting to release lock for key: $lockKey with token: $fencingToken" }

        return redisTemplate.execute(releaseLockScript, listOf(lockKey), listOf(fencingToken))
            .next()
            .map { result ->
                val success = result == 1L
                if (success) {
                    logger.debug { "Successfully released lock for key: $lockKey" }
                } else {
                    logger.warn { "Failed to release lock for key: $lockKey (token mismatch or lock not found)" }
                }
                success
            }
            .onErrorReturn { error ->
                logger.error(error) { "Error releasing lock for key: $lockKey" }
                false
            }
    }

    fun extendLock(lockKey: String, fencingToken: String, ttlSeconds: Long): Mono<Boolean> {
        logger.debug { "Attempting to extend lock for key: $lockKey with token: $fencingToken" }

        // Lua script for atomic lock extension with fencing token verification
        val extendLockScript = """
            if redis.call("GET", KEYS[1]) == ARGV[1] then
                return redis.call("EXPIRE", KEYS[1], ARGV[2])
            else
                return 0
            end
        """.trimIndent()

        val script = RedisScript.of(extendLockScript, Long::class.java)

        return redisTemplate.execute(script, listOf(lockKey), listOf(fencingToken, ttlSeconds.toString()))
            .next()
            .map { result ->
                val success = result == 1L
                if (success) {
                    logger.debug { "Successfully extended lock for key: $lockKey" }
                } else {
                    logger.warn { "Failed to extend lock for key: $lockKey (token mismatch or lock not found)" }
                }
                success
            }
            .onErrorReturn { error ->
                logger.error(error) { "Error extending lock for key: $lockKey" }
                false
            }
    }

    fun isLocked(showId: Long, seatId: String): Mono<Boolean> {
        val lockKey = generateLockKey(showId, seatId)
        return redisTemplate.hasKey(lockKey)
            .onErrorReturn(false)
    }

    fun getLockInfo(showId: Long, seatId: String): Mono<LockInfo?> {
        val lockKey = generateLockKey(showId, seatId)

        return redisTemplate.opsForValue()
            .get(lockKey)
            .zipWith(redisTemplate.getExpire(lockKey))
            .map { (fencingToken, ttl) ->
                LockInfo(lockKey, fencingToken, ttl.seconds)
            }
            .onErrorReturn(null)
    }

    private fun generateLockKey(showId: Long, seatId: String): String {
        return "$LOCK_PREFIX$showId:$seatId"
    }

    private fun generateFencingToken(): String {
        return "${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8)}"
    }
}

sealed class LockResult {
    abstract val lockKey: String

    data class Success(
        override val lockKey: String,
        val fencingToken: String,
        val ttlSeconds: Long
    ) : LockResult()

    data class AlreadyLocked(
        override val lockKey: String
    ) : LockResult()

    data class Error(
        override val lockKey: String,
        val message: String
    ) : LockResult()
}

data class LockInfo(
    val lockKey: String,
    val fencingToken: String,
    val remainingTtlSeconds: Long
)