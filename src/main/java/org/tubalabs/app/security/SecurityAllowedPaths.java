package org.tubalabs.app.security;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

public final class SecurityAllowedPaths {

    public static final String LOGIN_PATH = "/login";
    public static final String LOCAL_LOGIN_PATH = "/login/local";
    public static final String REGISTER_PATH = "/register";
    public static final String ERROR_PATH = "/error";
    public static final String PROFILE_PATH = "/profile";
    public static final String LOGOUT_PATH = "/logout";
    public static final String LOCAL_LOGIN_API_PATH = "/api/local-login";

    public static final String[] CORE_PUBLIC_MATCHERS = {
            LOGIN_PATH,
            ERROR_PATH,
            "/css/**",
            "/js/**"
    };
    public static final String[] PASSWORD_PUBLIC_MATCHERS = {
            LOCAL_LOGIN_PATH,
            REGISTER_PATH,
            LOCAL_LOGIN_API_PATH
    };
    public static final String[] OAUTH2_PUBLIC_MATCHERS = {
            "/oauth2/authorization/**"
    };
    public static final String[] CSRF_IGNORED_MATCHERS = {
            "/api/**"
    };

    private static final List<String> PUBLIC_MATCHERS = combinedPublicMatchers();
    private static final List<String> PROFILE_SETUP_ALLOWED_MATCHERS = List.of(
            PROFILE_PATH,
            PROFILE_PATH + "/**",
            LOGOUT_PATH,
            "/actuator/**",
            "/profile-pictures/**");

    private SecurityAllowedPaths() {
    }

    public static boolean isProfileSetupAllowedPath(@NonNull String path) {
        return matchesAny(path, PUBLIC_MATCHERS) || matchesAny(path, PROFILE_SETUP_ALLOWED_MATCHERS);
    }

    private static List<String> combinedPublicMatchers() {
        final List<String> matchers = new ArrayList<>();
        matchers.addAll(List.of(CORE_PUBLIC_MATCHERS));
        matchers.addAll(List.of(PASSWORD_PUBLIC_MATCHERS));
        matchers.addAll(List.of(OAUTH2_PUBLIC_MATCHERS));
        return List.copyOf(matchers);
    }

    private static boolean matchesAny(String path, List<String> matchers) {
        for (String matcher : matchers) {
            if (matches(path, matcher)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(String path, String matcher) {
        if (!matcher.endsWith("/**")) {
            return path.equals(matcher);
        }

        final String prefix = matcher.substring(0, matcher.length() - 3);
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
