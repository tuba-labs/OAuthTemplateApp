package org.tubalabs.app.email;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.javamail.JavaMailSender;
import org.tubalabs.app.email.config.EmailConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.propertyValues;

class EmailConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(EmailConfig.class)
            .withPropertyValues(propertyValues());

    @Test
    void createsJavaMailEmailTransportWhenJavaMailSenderExists() {
        contextRunner
                .withBean(JavaMailSender.class, () -> Mockito.mock(JavaMailSender.class))
                .run(context -> assertThat(context.getBean(EmailTransport.class))
                        .isInstanceOf(JavaMailEmailSender.class));
    }

    @Test
    void doesNotCreateEmailTransportWhenJavaMailSenderIsMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(EmailTransport.class));
    }
}
