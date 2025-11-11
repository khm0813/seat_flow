package com.seatflow.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @Primary
    fun reactiveRedisTemplate(factory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, String>(StringRedisSerializer())
            .build()
        return ReactiveRedisTemplate(factory, serializationContext)
    }

    @Bean
    fun reactiveRedisMessageListenerContainer(
        factory: ReactiveRedisConnectionFactory
    ): ReactiveRedisMessageListenerContainer {
        return ReactiveRedisMessageListenerContainer(factory)
    }
}