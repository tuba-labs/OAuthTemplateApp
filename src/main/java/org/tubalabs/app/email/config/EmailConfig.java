package org.tubalabs.app.email.config;

import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.tubalabs.app.email.EmailTransport;
import org.tubalabs.app.email.JavaMailEmailSender;
import org.tubalabs.app.email.outbox.EmailOutboxDispatchScheduler;
import org.tubalabs.app.email.outbox.EmailOutboxDispatcher;
import org.tubalabs.app.email.outbox.db.EmailOutboxRepository;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(EmailSenderProperties.class)
public class EmailConfig {

    @Bean
    public ThreadPoolTaskScheduler emailOutboxTaskScheduler(@NonNull EmailSenderProperties emailSenderProperties) {
        final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(emailSenderProperties.dispatchSchedulerPoolSize());
        taskScheduler.setThreadNamePrefix("email-outbox-");
        return taskScheduler;
    }

    @Bean
    @ConditionalOnBean(JavaMailSender.class)
    public EmailTransport javaMailEmailTransport(@NonNull JavaMailSender javaMailSender,
                                                 @NonNull EmailSenderProperties emailSenderProperties) {
        return new JavaMailEmailSender(javaMailSender, emailSenderProperties);
    }

    @Bean
    @ConditionalOnBean({EmailTransport.class, EmailOutboxRepository.class, Clock.class})
    public EmailOutboxDispatcher emailOutboxDispatcher(@NonNull EmailOutboxRepository emailOutboxRepository,
                                                       @NonNull EmailTransport emailTransport,
                                                       @NonNull EmailSenderProperties emailSenderProperties,
                                                       @NonNull Clock clock) {
        return new EmailOutboxDispatcher(emailOutboxRepository, emailTransport, emailSenderProperties, clock);
    }

    @Bean
    @ConditionalOnBean(EmailOutboxDispatcher.class)
    public EmailOutboxDispatchScheduler emailOutboxDispatchScheduler(
            @NonNull ThreadPoolTaskScheduler emailOutboxTaskScheduler,
            @NonNull EmailOutboxDispatcher emailOutboxDispatcher,
            @NonNull EmailSenderProperties emailSenderProperties) {
        return new EmailOutboxDispatchScheduler(
                emailOutboxTaskScheduler, emailOutboxDispatcher, emailSenderProperties);
    }
}
