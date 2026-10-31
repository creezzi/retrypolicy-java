package io.github.creezzi.retrypolicy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/** Immutable retry configuration. Build with {@link #builder()}. */
public final class RetryPolicy {
    /** Strategy for waiting between attempts; replaceable in tests. */
    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final double multiplier;
    private final List<Class<? extends Throwable>> retryOn;
    private final Sleeper sleeper;

    private RetryPolicy(Builder b) {
        this.maxAttempts = b.maxAttempts;
        this.baseDelay = b.baseDelay;
        this.maxDelay = b.maxDelay;
        this.multiplier = b.multiplier;
        this.retryOn = List.copyOf(b.retryOn);
        this.sleeper = b.sleeper;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Delay applied after the given failed attempt (1-based). */
    Duration delayAfter(int attempt) {
        double millis = baseDelay.toMillis() * Math.pow(multiplier, attempt - 1);
        return Duration.ofMillis((long) Math.min(millis, maxDelay.toMillis()));
    }

    private boolean shouldRetry(Throwable t) {
        return retryOn.stream().anyMatch(type -> type.isInstance(t));
    }

    public <T> T call(Callable<T> task) throws Exception {
        for (int attempt = 1; ; attempt++) {
            try {
                return task.call();
            } catch (Exception e) {
                if (!shouldRetry(e)) {
                    throw e;
                }
                if (attempt >= maxAttempts) {
                    throw new RetryExhaustedException(attempt, e);
                }
                sleeper.sleep(delayAfter(attempt));
            }
        }
    }

    public static final class Builder {
        private int maxAttempts = 3;
        private Duration baseDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double multiplier = 2.0;
        private final java.util.ArrayList<Class<? extends Throwable>> retryOn = new java.util.ArrayList<>();
        private Sleeper sleeper = d -> Thread.sleep(d.toMillis());

        public Builder maxAttempts(int value) {
            if (value < 1) {
                throw new IllegalArgumentException("maxAttempts must be >= 1");
            }
            this.maxAttempts = value;
            return this;
        }

        public Builder baseDelay(Duration value) {
            this.baseDelay = value;
            return this;
        }

        public Builder maxDelay(Duration value) {
            this.maxDelay = value;
            return this;
        }

        public Builder multiplier(double value) {
            this.multiplier = value;
            return this;
        }

        @SafeVarargs
        public final Builder retryOn(Class<? extends Throwable>... types) {
            retryOn.addAll(List.of(types));
            return this;
        }

        public Builder sleeper(Sleeper value) {
            this.sleeper = value;
            return this;
        }

        public RetryPolicy build() {
            if (retryOn.isEmpty()) {
                retryOn.add(Exception.class);
            }
            return new RetryPolicy(this);
        }
    }
}
