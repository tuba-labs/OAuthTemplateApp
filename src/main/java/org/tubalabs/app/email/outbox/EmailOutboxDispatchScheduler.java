package org.tubalabs.app.email.outbox;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.tubalabs.app.email.config.EmailSenderProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@RequiredArgsConstructor
public class EmailOutboxDispatchScheduler implements SmartLifecycle {

    private final @NonNull ThreadPoolTaskScheduler taskScheduler;
    private final @NonNull EmailOutboxDispatcher emailOutboxDispatcher;
    private final @NonNull EmailSenderProperties emailSenderProperties;

    private final List<ScheduledFuture<?>> scheduledDispatches = new ArrayList<>();
    private boolean running;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        final int workerCount = emailSenderProperties.dispatchSchedulerPoolSize();
        if (workerCount < 1) {
            throw new IllegalStateException("Email outbox dispatch scheduler pool size must be at least 1");
        }
        for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
            scheduledDispatches.add(taskScheduler.scheduleWithFixedDelay(
                    this::dispatchDueEmails,
                    emailSenderProperties.dispatchFixedDelay()));
        }
        running = true;
    }

    @Override
    public synchronized void stop() {
        for (ScheduledFuture<?> scheduledDispatch : scheduledDispatches) {
            scheduledDispatch.cancel(false);
        }
        scheduledDispatches.clear();
        running = false;
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    private void dispatchDueEmails() {
        try {
            emailOutboxDispatcher.dispatchDueEmails();
        } catch (RuntimeException exception) {
            log.error("Email outbox dispatch failed", exception);
        }
    }
}
