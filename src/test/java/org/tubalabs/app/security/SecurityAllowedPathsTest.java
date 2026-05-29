package org.tubalabs.app.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAllowedPathsTest {

    @Test
    void allowsSupportPathsDuringProfileSetup() {
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/css/app.css")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/js/theme.js")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/error")).isTrue();
    }

    @Test
    void allowsProfileSetupFlowPaths() {
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile-pictures/user.jpg")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/logout")).isTrue();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/language")).isTrue();
    }

    @Test
    void doesNotAllowOtherApplicationPathsDuringProfileSetup() {
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile/login-types")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/profile/password")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/login")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/login/local")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/register")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/oauth2/authorization/google")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/api/local-users")).isFalse();
        assertThat(SecurityAllowedPaths.isProfileSetupAllowedPath("/api/private")).isFalse();
    }
}
