package org.tubalabs.app.email;

import org.tubalabs.app.email.config.EmailSenderProperties;

import java.time.Duration;

public final class EmailSenderPropertiesTestFactory {

    public static final String FROM_ADDRESS = "no-reply@example.com";
    public static final String FROM_NAME = "OAuth Template App";
    public static final String MESSAGE_ID_DOMAIN = "example.com";
    public static final Duration LATEST_DELIVERY_DURATION = Duration.ofMinutes(15);
    public static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(10);
    public static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);
    public static final Duration DISPATCH_LOCK_DURATION = Duration.ofMinutes(2);
    public static final Duration DISPATCH_FIXED_DELAY = Duration.ofSeconds(10);
    public static final int DISPATCH_BATCH_SIZE = 25;
    public static final int DISPATCH_SCHEDULER_POOL_SIZE = 5;

    private EmailSenderPropertiesTestFactory() {
    }

    public static EmailSenderProperties emailSenderProperties() {
        return emailSenderProperties(
                MESSAGE_ID_DOMAIN,
                LATEST_DELIVERY_DURATION,
                INITIAL_RETRY_DELAY,
                MAX_RETRY_DELAY,
                DISPATCH_LOCK_DURATION,
                DISPATCH_FIXED_DELAY,
                DISPATCH_BATCH_SIZE,
                DISPATCH_SCHEDULER_POOL_SIZE);
    }

    public static EmailSenderProperties emailSenderProperties(String messageIdDomain,
                                                             Duration latestDeliveryDuration,
                                                             Duration initialRetryDelay,
                                                             Duration maxRetryDelay,
                                                             Duration dispatchLockDuration,
                                                             Duration dispatchFixedDelay,
                                                             int dispatchBatchSize,
                                                             int dispatchSchedulerPoolSize) {
        return new EmailSenderProperties(
                FROM_ADDRESS,
                FROM_NAME,
                messageIdDomain,
                latestDeliveryDuration,
                initialRetryDelay,
                maxRetryDelay,
                dispatchLockDuration,
                dispatchFixedDelay,
                dispatchBatchSize,
                dispatchSchedulerPoolSize);
    }

    public static String[] propertyValues() {
        return new String[] {
                "app.communication.email.from-address=" + FROM_ADDRESS,
                "app.communication.email.from-name=" + FROM_NAME,
                "app.communication.email.message-id-domain=" + MESSAGE_ID_DOMAIN,
                "app.communication.email.latest-delivery-duration=" + LATEST_DELIVERY_DURATION,
                "app.communication.email.initial-retry-delay=" + INITIAL_RETRY_DELAY,
                "app.communication.email.max-retry-delay=" + MAX_RETRY_DELAY,
                "app.communication.email.dispatch-lock-duration=" + DISPATCH_LOCK_DURATION,
                "app.communication.email.dispatch-fixed-delay=" + DISPATCH_FIXED_DELAY,
                "app.communication.email.dispatch-batch-size=" + DISPATCH_BATCH_SIZE,
                "app.communication.email.dispatch-scheduler-pool-size=" + DISPATCH_SCHEDULER_POOL_SIZE
        };
    }
}
