package io.github.creezzi.retrypolicy;

/** Thrown when every attempt failed; the last failure is the cause. */
public class RetryExhaustedException extends Exception {
    private final int attempts;

    public RetryExhaustedException(int attempts, Throwable cause) {
        super("gave up after " + attempts + " attempts", cause);
        this.attempts = attempts;
    }

    public int attempts() {
        return attempts;
    }
}
