package io.github.creezzi.retrypolicy;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {
    private final List<Duration> sleeps = new ArrayList<>();

    private RetryPolicy.Builder policy() {
        return RetryPolicy.builder().sleeper(sleeps::add);
    }

    @Test
    void succeedsAfterTransientFailures() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        String result = policy().maxAttempts(4).retryOn(IOException.class).build().call(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new IOException("flaky");
            }
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, calls.get());
        assertEquals(2, sleeps.size());
    }

    @Test
    void exhaustionCarriesTheLastCause() {
        RetryExhaustedException e = assertThrows(RetryExhaustedException.class,
                () -> policy().maxAttempts(2).build().call(() -> { throw new IllegalStateException("x"); }));
        assertEquals(2, e.attempts());
        assertInstanceOf(IllegalStateException.class, e.getCause());
    }

    @Test
    void nonMatchingExceptionsPropagateImmediately() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> policy().retryOn(IOException.class).build().call(() -> {
            calls.incrementAndGet();
            throw new IllegalArgumentException();
        }));
        assertEquals(1, calls.get());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void backoffGrowsAndIsCapped() throws Exception {
        assertThrows(RetryExhaustedException.class, () -> policy()
                .maxAttempts(5).baseDelay(Duration.ofMillis(100)).maxDelay(Duration.ofMillis(300))
                .build().call(() -> { throw new RuntimeException(); }));
        assertEquals(List.of(Duration.ofMillis(100), Duration.ofMillis(200),
                Duration.ofMillis(300), Duration.ofMillis(300)), sleeps);
    }

    @Test
    void rejectsZeroAttempts() {
        assertThrows(IllegalArgumentException.class, () -> RetryPolicy.builder().maxAttempts(0));
    }
}
