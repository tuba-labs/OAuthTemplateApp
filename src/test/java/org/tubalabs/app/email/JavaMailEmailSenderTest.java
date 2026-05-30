package org.tubalabs.app.email;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.tubalabs.app.email.config.EmailSenderProperties;
import org.tubalabs.app.email.exceptions.EmailDeliveryException;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import static org.tubalabs.app.email.EmailSenderPropertiesTestFactory.emailSenderProperties;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaMailEmailSenderTest {

    private static final String RECIPIENT = "person@example.com";
    private static final String SUBJECT = "Test email";
    private static final String BODY = "This is a test email.";
    private static final String FROM_ADDRESS = "no-reply@example.com";
    private static final String FROM_NAME = "OAuth Template App";
    private static final String MESSAGE_ID_DOMAIN = "example.com";
    private static final String UNEXPECTED_FAILURE_MESSAGE = "Unexpected JavaMail failure";
    private static final UUID EMAIL_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String MESSAGE_ID = "<55555555-5555-5555-5555-555555555555@" + MESSAGE_ID_DOMAIN + ">";
    private static final EmailMessage MESSAGE = new EmailMessage(RECIPIENT, SUBJECT, BODY);

    private final JavaMailSender javaMailSender = Mockito.mock(JavaMailSender.class);
    private final EmailSenderProperties emailSenderProperties = emailSenderProperties(
            MESSAGE_ID_DOMAIN,
            Duration.ofMinutes(15),
            Duration.ofSeconds(10),
            Duration.ofMinutes(5),
            Duration.ofMinutes(2),
            Duration.ofSeconds(10),
            25,
            5);
    private final MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
    private final JavaMailEmailSender emailTransport =
            new JavaMailEmailSender(javaMailSender, emailSenderProperties);

    @BeforeEach
    void setUp() {
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendsPlainTextMimeMessageThroughJavaMail() throws Exception {
        emailTransport.deliver(EMAIL_ID, MESSAGE);

        assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(RECIPIENT);
        assertThat(mimeMessage.getSubject()).isEqualTo(SUBJECT);
        assertThat(mimeMessage.getContent()).isEqualTo(BODY);
        final InternetAddress from = (InternetAddress) mimeMessage.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo(FROM_ADDRESS);
        assertThat(from.getPersonal()).isEqualTo(FROM_NAME);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendsConfiguredMessageIdHeader() throws Exception {
        emailTransport.deliver(EMAIL_ID, MESSAGE);

        assertThat(mimeMessage.getHeader("Message-ID")).containsExactly(MESSAGE_ID);
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void wrapsMailSendFailures() {
        Mockito.doThrow(new MailSendException("SMTP unavailable"))
                .when(javaMailSender)
                .send(mimeMessage);

        assertThatThrownBy(() -> emailTransport.deliver(EMAIL_ID, MESSAGE))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessage("Could not deliver email to " + RECIPIENT)
                .hasCauseInstanceOf(MailSendException.class);
    }

    @Test
    void wrapsUnexpectedMailAdapterFailures() {
        final RuntimeException failure = new RuntimeException(UNEXPECTED_FAILURE_MESSAGE);
        when(javaMailSender.createMimeMessage()).thenThrow(failure);

        assertThatThrownBy(() -> emailTransport.deliver(EMAIL_ID, MESSAGE))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessage("Could not deliver email to " + RECIPIENT)
                .hasCause(failure);
    }
}
