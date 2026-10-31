# retrypolicy

Retry any callable with exponential backoff, a delay cap and selectable exceptions.

## Usage

```java
RetryPolicy policy = RetryPolicy.builder()
        .maxAttempts(5)
        .baseDelay(Duration.ofMillis(200))
        .retryOn(IOException.class)
        .build();
String body = policy.call(() -> client.fetch(url));
```

## Test

```bash
mvn test
```

## License

MIT
