package org.tubalabs.app.users.identity.externalidentity.providers.github;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GithubEmailClientTest {

    private static final String PRIMARY_EMAIL = "primary@example.com";
    private static final String SECONDARY_EMAIL = "secondary@example.com";
    private static final String UNVERIFIED_EMAIL = "unverified@example.com";

    private final GithubEmailClient githubEmailClient = new GithubEmailClient(RestClient.builder().build());

    @Test
    void prefersVerifiedPrimaryEmail() {
        final Optional<String> email = githubEmailClient.preferredVerifiedEmail(List.of(
                new GithubEmailAddress(SECONDARY_EMAIL, false, true),
                new GithubEmailAddress(PRIMARY_EMAIL, true, true)));

        assertThat(email).contains(PRIMARY_EMAIL);
    }

    @Test
    void fallsBackToFirstVerifiedEmail() {
        final Optional<String> email = githubEmailClient.preferredVerifiedEmail(List.of(
                new GithubEmailAddress(UNVERIFIED_EMAIL, true, false),
                new GithubEmailAddress(SECONDARY_EMAIL, false, true)));

        assertThat(email).contains(SECONDARY_EMAIL);
    }

    @Test
    void ignoresUnverifiedEmails() {
        final Optional<String> email = githubEmailClient.preferredVerifiedEmail(List.of(
                new GithubEmailAddress(UNVERIFIED_EMAIL, true, false)));

        assertThat(email).isEmpty();
    }
}
