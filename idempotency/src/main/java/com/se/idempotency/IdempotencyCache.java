package com.se.idempotency;

import org.aspectj.apache.bcel.generic.RET;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class IdempotencyCache {

    private final static Duration TTL = Duration.ofMinutes(10);
    private final StringRedisTemplate redisTemplate;

    public IdempotencyCache(StringRedisTemplate redis){
        this.redisTemplate = redis;
    }


    public boolean claim(String idempotencyKey){
        boolean firstToClaim = redisTemplate.opsForValue()
                .setIfAbsent("idempotenct:" + idempotencyKey, "1", TTL);

        return Boolean.TRUE.equals(firstToClaim);
    }
}
