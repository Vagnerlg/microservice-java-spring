package com.github.vagnerlg.cart.infrastructure.redis;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.github.vagnerlg.cart.domain.Cart;
import com.github.vagnerlg.cart.domain.CartRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class RedisCartRepository implements CartRepository {

    private static final String KEY_PREFIX = "cart:";
    private static final Duration TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    RedisCartRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Cart findByUserId(String userId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (json == null) {
            return Cart.empty(userId);
        }
        try {
            return objectMapper.readValue(json, Cart.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize cart for user: " + userId, e);
        }
    }

    @Override
    public Cart save(Cart cart) {
        try {
            String json = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(KEY_PREFIX + cart.userId(), json, TTL);
            return cart;
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize cart for user: " + cart.userId(), e);
        }
    }

    @Override
    public void deleteByUserId(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
