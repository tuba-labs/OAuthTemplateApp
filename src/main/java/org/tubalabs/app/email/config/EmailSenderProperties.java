package org.tubalabs.app.email.config;

import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.communication.email")
public record EmailSenderProperties(
        @NonNull String fromAddress,
        @NonNull String fromName,
        @NonNull String messageIdDomain,
        @NonNull Duration latestDeliveryDuration,
        @NonNull Duration initialRetryDelay,
        @NonNull Duration maxRetryDelay,
        @NonNull Duration dispatchLockDuration,
        @NonNull Duration dispatchFixedDelay,
        int dispatchBatchSize,
        int dispatchSchedulerPoolSize) {

    public EmailSenderProperties {
        fromAddress = requiredText(fromAddress, "fromAddress");
        fromName = requiredText(fromName, "fromName");
        messageIdDomain = requiredText(messageIdDomain, "messageIdDomain");
        latestDeliveryDuration = requiredDuration(latestDeliveryDuration, "latestDeliveryDuration");
        initialRetryDelay = requiredDuration(initialRetryDelay, "initialRetryDelay");
        maxRetryDelay = requiredDuration(maxRetryDelay, "maxRetryDelay");
        dispatchLockDuration = requiredDuration(dispatchLockDuration, "dispatchLockDuration");
        dispatchFixedDelay = requiredDuration(dispatchFixedDelay, "dispatchFixedDelay");
        if (dispatchBatchSize < 1) {
            throw new IllegalArgumentException("dispatchBatchSize must be at least 1");
        }
        if (dispatchSchedulerPoolSize < 1) {
            throw new IllegalArgumentException("dispatchSchedulerPoolSize must be at least 1");
        }
    }

    private static String requiredText(String value, String fieldName) {
        final String text = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return text;
    }

    private static Duration requiredDuration(Duration value, String fieldName) {
        final Duration duration = Objects.requireNonNull(value, fieldName + " cannot be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return duration;
    }
}
