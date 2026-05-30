package org.tubalabs.app.email;

import jakarta.mail.internet.MimeMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.tubalabs.app.email.config.EmailSenderProperties;
import org.tubalabs.app.email.exceptions.EmailDeliveryException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RequiredArgsConstructor
public class JavaMailEmailSender implements EmailTransport {

    private final @NonNull JavaMailSender javaMailSender;
    private final @NonNull EmailSenderProperties emailSenderProperties;

    @Override
    public void deliver(@NonNull UUID emailId, @NonNull EmailMessage message) {
        try {
            final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    false,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(emailSenderProperties.fromAddress(), emailSenderProperties.fromName());
            helper.setTo(message.recipient());
            helper.setSubject(message.subject());
            helper.setText(message.body(), false);
            mimeMessage.setHeader("Message-ID", messageId(emailId));
            javaMailSender.send(mimeMessage);
        } catch (Exception exception) {
            throw new EmailDeliveryException("Could not deliver email to " + message.recipient(), exception);
        }
    }


    private String messageId(@NonNull UUID emailId) {
        return "<" + emailId + "@" + messageIdDomain() + ">";
    }

    private String messageIdDomain() {
        return emailSenderProperties.messageIdDomain();
    }
}
