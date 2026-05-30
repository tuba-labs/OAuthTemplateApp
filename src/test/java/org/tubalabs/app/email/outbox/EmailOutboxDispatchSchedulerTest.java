package org.tubalabs.app.email.outbox;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.tubalabs.app.email.EmailSenderPropertiesTestFactory;
import org.tubalabs.app.email.config.EmailSenderProperties;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.INITIAL_RETRY_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.LATEST_DELIVERY_DURATION;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.MAX_RETRY_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.MESSAGE_ID_DOMAIN;
import static org.mockito.Mockito.when;

class EmailOutboxDispatchSchedulerTest {

    private static final Duration DISPATCH_FIXED_DELAY = Duration.ofMillis(10);
    private static final Duration DISPATCH_LOCK_DURATION = Duration.ofMinutes(2);
    private static final int WORKER_COUNT = 3;
    private static final int DISPATCH_BATCH_SIZE = 25;

    @Test
    void startsConfiguredDispatchWorkersAndStopsThem() throws Exception {
        final ThreadPoolTaskScheduler taskScheduler = taskScheduler(WORKER_COUNT);
        final EmailOutboxDispatcher emailOutboxDispatcher = Mockito.mock(EmailOutboxDispatcher.class);
        final CountDownLatch dispatchWorkersStarted = new CountDownLatch(WORKER_COUNT);
        final CountDownLatch releaseDispatchWorkers = new CountDownLatch(1);
        when(emailOutboxDispatcher.dispatchDueEmails()).thenAnswer(invocation -> {
            dispatchWorkersStarted.countDown();
            try {
                releaseDispatchWorkers.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return 0;
        });
        final EmailOutboxDispatchScheduler dispatchScheduler = new EmailOutboxDispatchScheduler(
                taskScheduler, emailOutboxDispatcher, emailSenderProperties(WORKER_COUNT));

        try {
            dispatchScheduler.start();

            assertThat(dispatchScheduler.isRunning()).isTrue();
            assertThat(dispatchWorkersStarted.await(5, TimeUnit.SECONDS)).isTrue();

            releaseDispatchWorkers.countDown();
            dispatchScheduler.stop();

            assertThat(dispatchScheduler.isRunning()).isFalse();
        } finally {
            releaseDispatchWorkers.countDown();
            dispatchScheduler.stop();
            taskScheduler.shutdown();
        }
    }

    @Test
    void keepsSchedulingAfterDispatchFailure() throws Exception {
        final ThreadPoolTaskScheduler taskScheduler = taskScheduler(1);
        final EmailOutboxDispatcher emailOutboxDispatcher = Mockito.mock(EmailOutboxDispatcher.class);
        final AtomicInteger dispatchAttempts = new AtomicInteger();
        final CountDownLatch successfulDispatchAfterFailure = new CountDownLatch(1);
        when(emailOutboxDispatcher.dispatchDueEmails()).thenAnswer(invocation -> {
            if (dispatchAttempts.incrementAndGet() == 1) {
                throw new RuntimeException("SMTP unavailable");
            }
            successfulDispatchAfterFailure.countDown();
            return 0;
        });
        final EmailOutboxDispatchScheduler dispatchScheduler = new EmailOutboxDispatchScheduler(
                taskScheduler, emailOutboxDispatcher, emailSenderProperties(1));

        try {
            dispatchScheduler.start();

            assertThat(successfulDispatchAfterFailure.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(dispatchScheduler.isRunning()).isTrue();
        } finally {
            dispatchScheduler.stop();
            taskScheduler.shutdown();
        }
    }

    private static ThreadPoolTaskScheduler taskScheduler(int poolSize) {
        final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(poolSize);
        taskScheduler.setThreadNamePrefix("email-outbox-test-");
        taskScheduler.initialize();
        return taskScheduler;
    }

    private static EmailSenderProperties emailSenderProperties(int dispatchSchedulerPoolSize) {
        return EmailSenderPropertiesTestFactory.emailSenderProperties(
                MESSAGE_ID_DOMAIN,
                LATEST_DELIVERY_DURATION,
                INITIAL_RETRY_DELAY,
                MAX_RETRY_DELAY,
                DISPATCH_LOCK_DURATION,
                DISPATCH_FIXED_DELAY,
                DISPATCH_BATCH_SIZE,
                dispatchSchedulerPoolSize);
    }
}
