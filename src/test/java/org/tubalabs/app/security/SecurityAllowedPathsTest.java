package org.tubalabs.app.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAllowedPathsTest {

    @Test
    void allowsPublicSecurityPathsDuringProfileSetup() {
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/login")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/login/local")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/register")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/css/app.css")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/js/theme.js")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/oauth2/authorization/google")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/api/local-users")).isTrue();
    }

    @Test
    void allowsProfileSetupFlowPaths() {
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile/edit")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile-pictures/user.jpg")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/logout")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/language")).isTrue();
    }

    @Test
    void doesNotAllowNonPublicApiPathDuringProfileSetup() {
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/api/private")).isFalse();
    }
}
