package org.tubalabs.app.users.password.validation.vetoers.password;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

@Slf4j
@Component
public class PwnedPasswordClient {


    private static final String ADD_PADDING_HEADER = "Add-Padding";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String ADD_PADDING_VALUE = "true";
    private static final String USER_AGENT = "OAuthTemplateApp";
    private static final int HASH_PREFIX_LENGTH = 5;

    private final RestClient restClient;
    private final boolean enabled;

    public PwnedPasswordClient(RestClient.Builder restClientBuilder,
                               @Value("${app.security.password.pwned-passwords.base-url}") String baseUrl,
                               @Value("${app.security.password.pwned-passwords.enabled:true}") boolean enabled) {

        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(USER_AGENT_HEADER, USER_AGENT)
                .build();
        this.enabled = enabled;
    }

    public boolean isPwned(@NonNull String password) {
        if (!enabled || password.isBlank()) {
            return false;
        }

        final PasswordHash passwordHash = passwordHash(password);
        try {
            final String responseBody = restClient.get()
                    .uri("/range/{hashPrefix}", passwordHash.prefix())
                    .header(ADD_PADDING_HEADER, ADD_PADDING_VALUE)
                    .retrieve()
                    .body(String.class);
            return containsPositiveSuffixMatch(responseBody, passwordHash.suffix());
        } catch (RestClientException exception) {
            log.warn("Could not check password against Have I Been Pwned; accepting password: {}", exception.getMessage());
            return false;
        }
    }

    private boolean containsPositiveSuffixMatch(String responseBody, String suffix) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        final String upperCaseSuffix = suffix.toUpperCase(Locale.ROOT);
        for (String line : responseBody.split("\\R")) {
            final int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }

            final String returnedSuffix = line.substring(0, separatorIndex).toUpperCase(Locale.ROOT);
            if (upperCaseSuffix.equals(returnedSuffix)) {
                return count(line.substring(separatorIndex + 1)) > 0;
            }
        }
        return false;
    }

    private int count(String countValue) {
        try {
            return Integer.parseInt(countValue.trim());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private PasswordHash passwordHash(String password) {
        try {
            final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            final String hash = HexFormat.of()
                    .formatHex(messageDigest.digest(password.getBytes(StandardCharsets.UTF_8)))
                    .toUpperCase(Locale.ROOT);
            return new PasswordHash(hash.substring(0, HASH_PREFIX_LENGTH), hash.substring(HASH_PREFIX_LENGTH));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 hashing is unavailable", exception);
        }
    }

    private record PasswordHash(String prefix, String suffix) {
    }
}
