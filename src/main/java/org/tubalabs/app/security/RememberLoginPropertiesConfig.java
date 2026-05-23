package org.tubalabs.app.security;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class RememberLoginPropertiesConfig {

    private static final Pattern SIMPLE_DURATION = Pattern.compile("^(\\d+)(ms|s|m|h|d)$");

    @Bean
    RememberLoginProperties rememberLoginProperties(
            @Value("${app.security.remember-login.prompt-skip-duration}") String promptSkipDuration,
            @Value("${app.security.remember-me.token-validity}") String tokenValidity) {

        return new RememberLoginProperties(
                parseDuration("app.security.remember-login.prompt-skip-duration", promptSkipDuration),
                parseDuration("app.security.remember-me.token-validity", tokenValidity));
    }

    private static Duration parseDuration(@NonNull String propertyName, @NonNull String rawValue) {
        final String trimmedValue = rawValue.trim();
        final String value = trimmedValue.toLowerCase(Locale.ROOT);
        final Matcher matcher = SIMPLE_DURATION.matcher(value);
        if (matcher.matches()) {
            final long amount = Long.parseLong(matcher.group(1));
            final String unit = matcher.group(2);
            return switch (unit) {
                case "ms" -> Duration.ofMillis(amount);
                case "s" -> Duration.ofSeconds(amount);
                case "m" -> Duration.ofMinutes(amount);
                case "h" -> Duration.ofHours(amount);
                case "d" -> Duration.ofDays(amount);
                default -> throw new IllegalStateException("Unsupported duration unit: " + unit);
            };
        }

        try {
            return Duration.parse(trimmedValue);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid duration for " + propertyName + ": " + rawValue, exception);
        }
    }
}
