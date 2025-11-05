package com.seatflow.infrastructure.lock

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.TestConstructor
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Duration

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RedisLockManagerTest(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {

    private val lockManager = RedisLockManager(redisTemplate)

    @Test
    fun `should acquire lock successfully when not exists`() {
        val showId = 1L
        val seatId = "A1"

        StepVerifier.create(lockManager.acquireLock(showId, seatId, 60))
            .expectNextMatches { result ->
                result is LockResult.Success &&
                        result.lockKey.contains("lock:seat:1:A1") &&
                        result.fencingToken.isNotEmpty() &&
                        result.ttlSeconds == 60L
            }
            .verifyComplete()
    }

    @Test
    fun `should fail to acquire lock when already exists`() {
        val showId = 1L
        val seatId = "A2"

        // First acquisition should succeed
        val firstAcquisition = lockManager.acquireLock(showId, seatId, 60)
            .cast(LockResult.Success::class.java)

        // Second acquisition should fail
        val secondAcquisition = firstAcquisition
            .then(lockManager.acquireLock(showId, seatId, 60))

        StepVerifier.create(secondAcquisition)
            .expectNextMatches { result ->
                result is LockResult.AlreadyLocked &&
                        result.lockKey.contains("lock:seat:1:A2")
            }
            .verifyComplete()
    }

    @Test
    fun `should release lock successfully with correct fencing token`() {
        val showId = 1L
        val seatId = "A3"

        val test = lockManager.acquireLock(showId, seatId, 60)
            .cast(LockResult.Success::class.java)
            .flatMap { result ->
                lockManager.releaseLock(result.lockKey, result.fencingToken)
            }

        StepVerifier.create(test)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should fail to release lock with incorrect fencing token`() {
        val showId = 1L
        val seatId = "A4"

        val test = lockManager.acquireLock(showId, seatId, 60)
            .cast(LockResult.Success::class.java)
            .flatMap { result ->
                lockManager.releaseLock(result.lockKey, "wrong-token")
            }

        StepVerifier.create(test)
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `should extend lock successfully with correct fencing token`() {
        val showId = 1L
        val seatId = "A5"

        val test = lockManager.acquireLock(showId, seatId, 60)
            .cast(LockResult.Success::class.java)
            .flatMap { result ->
                lockManager.extendLock(result.lockKey, result.fencingToken, 120)
            }

        StepVerifier.create(test)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should check if lock exists`() {
        val showId = 1L
        val seatId = "A6"

        val test = lockManager.acquireLock(showId, seatId, 60)
            .then(lockManager.isLocked(showId, seatId))

        StepVerifier.create(test)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should get lock info when lock exists`() {
        val showId = 1L
        val seatId = "A7"

        val test = lockManager.acquireLock(showId, seatId, 60)
            .cast(LockResult.Success::class.java)
            .flatMap { result ->
                lockManager.getLockInfo(showId, seatId)
                    .map { lockInfo ->
                        lockInfo != null &&
                                lockInfo.lockKey == result.lockKey &&
                                lockInfo.fencingToken == result.fencingToken &&
                                lockInfo.remainingTtlSeconds > 0
                    }
            }

        StepVerifier.create(test)
            .expectNext(true)
            .verifyComplete()
    }

    @Test
    fun `should handle concurrent lock attempts`() {
        val showId = 1L
        val seatId = "A8"

        // Simulate concurrent lock attempts
        val lock1 = lockManager.acquireLock(showId, seatId, 60)
        val lock2 = lockManager.acquireLock(showId, seatId, 60)
        val lock3 = lockManager.acquireLock(showId, seatId, 60)

        val combined = Mono.zip(lock1, lock2, lock3)

        StepVerifier.create(combined)
            .expectNextMatches { (result1, result2, result3) ->
                // Only one should succeed, others should be AlreadyLocked
                val results = listOf(result1, result2, result3)
                val successCount = results.count { it is LockResult.Success }
                val alreadyLockedCount = results.count { it is LockResult.AlreadyLocked }

                successCount == 1 && alreadyLockedCount == 2
            }
            .verifyComplete()
    }

    @Test
    fun `should handle lock expiration`() {
        val showId = 1L
        val seatId = "A9"

        val test = lockManager.acquireLock(showId, seatId, 1) // 1 second TTL
            .then(Mono.delay(Duration.ofSeconds(2))) // Wait for expiration
            .then(lockManager.isLocked(showId, seatId))

        StepVerifier.create(test)
            .expectNext(false)
            .verifyComplete()
    }
}