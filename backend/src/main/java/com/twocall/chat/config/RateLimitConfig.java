package com.twocall.chat.config;

import com.twocall.chat.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitConfig {

    private final int maxRequestsPerMinute;
    private final ConcurrentHashMap<String, WindowCounter> requestCounts = new ConcurrentHashMap<>();

    public RateLimitConfig(@Value("${app.pairing.rate-limit-per-minute:10}") int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public void checkRateLimit(String key) {
        long currentMinute = Instant.now().getEpochSecond() / 60;
        WindowCounter counter = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || existing.minute != currentMinute) {
                return new WindowCounter(currentMinute, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (counter.counter.get() > maxRequestsPerMinute) {
            throw new RateLimitExceededException("Too many pairing requests. Please wait a minute before trying again.");
        }

        // Cleanup occasionally if size exceeds threshold
        if (requestCounts.size() > 5000) {
            requestCounts.entrySet().removeIf(entry -> entry.getValue().minute < currentMinute - 2);
        }
    }

    private static class WindowCounter {
        final long minute;
        final AtomicInteger counter;

        WindowCounter(long minute, AtomicInteger counter) {
            this.minute = minute;
            this.counter = counter;
        }
    }
}
