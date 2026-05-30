package org.tubalabs.app.email.outbox;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.email.EmailMessage;
import org.tubalabs.app.email.EmailSenderPropertiesTestFactory;
import org.tubalabs.app.email.EmailTransport;
import org.tubalabs.app.email.config.EmailSenderProperties;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxElementDeliveryDbo;
import org.tubalabs.app.email.outbox.db.EmailOutboxRepository;
import org.tubalabs.app.etc.db.SqlColumnNameResolver;
import org.tubalabs.app.etc.db.SqlRecordIntrospector;
import org.tubalabs.app.testtools.AbstractJdbcTestBaseTestClass;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.DISPATCH_FIXED_DELAY;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.LATEST_DELIVERY_DURATION;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        SqlColumnNameResolver.class,
        SqlRecordIntrospector.class,
        EmailOutboxRepository.class
})
class EmailOutboxDispatcherConcurrencyTest extends AbstractJdbcTestBaseTestClass {

    private static final int EMAIL_COUNT = 1_000;
    private static final int WORKER_COUNT = 5;
    private static final int CONNECTION_POOL_SIZE = 8;
    private static final int DISPATCH_BATCH_SIZE = 1;
    private static final String RECIPIENT_DOMAIN = "example.com";
    private static final String SUBJECT = "Concurrent email";
    private static final String BODY = "This email is sent by the outbox concurrency test.";
    private static final Instant NOW = Instant.parse("2026-05-29T12:00:00Z");
    private static final Instant LATEST_DELIVERY_TIME = NOW.plus(Duration.ofHours(1));
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(10);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);
    private static final Duration WORKER_TIMEOUT = Duration.ofSeconds(60);
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Autowired
    private EmailOutboxRepository emailOutboxRepository;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureConcurrencyTest(DynamicPropertyRegistry registry) {
        overrideHikariMaximumPoolSize(CONNECTION_POOL_SIZE);
    }

    @AfterAll
    static void clearConnectionPoolOverride() {
        clearHikariMaximumPoolSizeOverride();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentDispatchersOnlySendEachEmailOnce() throws Exception {
        assertConnectionPoolSize();

        final RecordingEmailTransport emailTransport = new RecordingEmailTransport(WORKER_COUNT);
        final EmailOutboxDispatcher emailOutboxDispatcher = new EmailOutboxDispatcher(
                emailOutboxRepository, emailTransport, emailSenderProperties(), CLOCK);
        final ExecutorService executorService = Executors.newFixedThreadPool(
                WORKER_COUNT, new NamedThreadFactory("email-outbox-concurrency-"));

        deleteOutboxEmails();
        try {
            insertPendingEmails();

            final CountDownLatch workersReady = new CountDownLatch(WORKER_COUNT);
            final CountDownLatch startDispatching = new CountDownLatch(1);
            final List<Future<Integer>> workerResults = startWorkers(
                    executorService, emailOutboxDispatcher, workersReady, startDispatching);

            assertThat(workersReady.await(10, TimeUnit.SECONDS)).isTrue();
            startDispatching.countDown();

            final int dispatchedEmailCount = dispatchedEmailCount(workerResults);

            assertThat(dispatchedEmailCount).isEqualTo(EMAIL_COUNT);
            assertThat(emailTransport.deliveredEmailIds()).hasSize(EMAIL_COUNT);
            assertThat(emailTransport.deliveriesByThread()).hasSize(WORKER_COUNT);
            assertThat(emailTransport.deliveriesByThread().values())
                    .allSatisfy(deliveryCount -> assertThat(deliveryCount.get()).isPositive());
            assertThat(sentEmailCount()).isEqualTo(EMAIL_COUNT);
        } finally {
            executorService.shutdownNow();
            deleteOutboxEmails();
        }
    }

    private void assertConnectionPoolSize() {
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        final HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        assertThat(hikariDataSource.getMaximumPoolSize()).isEqualTo(CONNECTION_POOL_SIZE);
    }

    private List<Future<Integer>> startWorkers(ExecutorService executorService,
                                               EmailOutboxDispatcher emailOutboxDispatcher,
                                               CountDownLatch workersReady,
                                               CountDownLatch startDispatching) {
        final List<Future<Integer>> workerResults = new ArrayList<>();
        for (int workerIndex = 0; workerIndex < WORKER_COUNT; workerIndex++) {
            workerResults.add(executorService.submit(() -> dispatchUntilAllEmailsAreSent(
                    emailOutboxDispatcher, workersReady, startDispatching)));
        }
        return workerResults;
    }

    private int dispatchUntilAllEmailsAreSent(EmailOutboxDispatcher emailOutboxDispatcher,
                                              CountDownLatch workersReady,
                                              CountDownLatch startDispatching) throws InterruptedException {
        workersReady.countDown();
        assertThat(startDispatching.await(10, TimeUnit.SECONDS)).isTrue();

        int dispatchedEmailCount = 0;
        while (sentEmailCount() < EMAIL_COUNT) {
            final int dispatchCount = emailOutboxDispatcher.dispatchDueEmails();
            dispatchedEmailCount += dispatchCount;
            if (dispatchCount == 0) {
                LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
            }
        }
        return dispatchedEmailCount;
    }

    private int dispatchedEmailCount(List<Future<Integer>> workerResults)
            throws InterruptedException, ExecutionException, TimeoutException {
        int dispatchedEmailCount = 0;
        for (Future<Integer> workerResult : workerResults) {
            dispatchedEmailCount += workerResult.get(WORKER_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }
        return dispatchedEmailCount;
    }

    private void insertPendingEmails() {
        for (int emailIndex = 0; emailIndex < EMAIL_COUNT; emailIndex++) {
            final UUID emailId = emailId(emailIndex);
            final EmailOutboxElementDbo element = EmailOutboxElementDbo.builder()
                    .id(emailId)
                    .recipient("person-" + emailIndex + "@" + RECIPIENT_DOMAIN)
                    .subject(SUBJECT)
                    .body(BODY)
                    .build();
            final EmailOutboxElementDeliveryDbo delivery = EmailOutboxElementDeliveryDbo.builder()
                    .emailOutboxElementId(emailId)
                    .deliveryStatus(EmailOutboxStatus.PENDING.name())
                    .attemptCount(0)
                    .nextAttemptAt(Timestamp.from(NOW))
                    .latestDeliveryAt(Timestamp.from(LATEST_DELIVERY_TIME))
                    .build();
            emailOutboxRepository.insert(element, delivery);
        }
    }

    private void deleteOutboxEmails() {
        jdbcClient.sql("DELETE FROM email_outbox_element").update();
    }

    private long sentEmailCount() {
        return jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM email_outbox_element_delivery
                        WHERE delivery_status = :delivery_status
                """)
                .param("delivery_status", EmailOutboxStatus.SENT.name())
                .query(Long.class)
                .single();
    }

    private static EmailSenderProperties emailSenderProperties() {
        return EmailSenderPropertiesTestFactory.emailSenderProperties(
                RECIPIENT_DOMAIN,
                LATEST_DELIVERY_DURATION,
                INITIAL_RETRY_DELAY,
                MAX_RETRY_DELAY,
                LOCK_DURATION,
                DISPATCH_FIXED_DELAY,
                DISPATCH_BATCH_SIZE,
                WORKER_COUNT);
    }

    private static UUID emailId(int emailIndex) {
        return new UUID(0L, emailIndex + 1L);
    }

    private static final class RecordingEmailTransport implements EmailTransport {

        private final CountDownLatch firstDeliveryByEachThread;
        private final ConcurrentHashMap<UUID, String> deliveredEmailIds = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, AtomicInteger> deliveriesByThread = new ConcurrentHashMap<>();

        private RecordingEmailTransport(int expectedThreadCount) {
            this.firstDeliveryByEachThread = new CountDownLatch(expectedThreadCount);
        }

        @Override
        public void deliver(UUID emailId, EmailMessage message) {
            final String threadName = Thread.currentThread().getName();
            final String previousThreadName = deliveredEmailIds.putIfAbsent(emailId, threadName);
            if (previousThreadName != null) {
                throw new AssertionError(
                        "Email " + emailId + " delivered by both " + previousThreadName + " and " + threadName);
            }
            final int threadDeliveryCount = deliveriesByThread
                    .computeIfAbsent(threadName, ignored -> new AtomicInteger())
                    .incrementAndGet();
            if (threadDeliveryCount == 1) {
                firstDeliveryByEachThread.countDown();
                waitForAllThreadsToDeliver();
            }
            LockSupport.parkNanos(Duration.ofMillis(1).toNanos());
        }

        private void waitForAllThreadsToDeliver() {
            try {
                if (!firstDeliveryByEachThread.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("Not all email outbox worker threads delivered an email");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting for email outbox workers", exception);
            }
        }

        private Map<UUID, String> deliveredEmailIds() {
            return deliveredEmailIds;
        }

        private Map<String, AtomicInteger> deliveriesByThread() {
            return deliveriesByThread;
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final String threadNamePrefix;
        private final AtomicInteger nextThreadNumber = new AtomicInteger(1);

        private NamedThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable task) {
            return new Thread(task, threadNamePrefix + nextThreadNumber.getAndIncrement());
        }
    }
}
