package org.tubalabs.app.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tubalabs.app.users.CurrentUserIdResolver;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class RememberLoginController {

    private static final String REMEMBER_LOGIN_VIEW = "org/tubalabs/app/security/remember-login";
    private static final String HOME_REDIRECT = "redirect:/";

    private final CurrentUserIdResolver currentUserIdResolver;
    private final RememberLoginPromptService rememberLoginPromptService;
    private final RememberMeServices rememberMeServices;

    @GetMapping("/remember-login")
    public String rememberLogin(@NonNull Authentication authentication) {
        if (authentication instanceof RememberMeAuthenticationToken) {
            return HOME_REDIRECT;
        }
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        if (!rememberLoginPromptService.shouldAsk(userId)) {
            return HOME_REDIRECT;
        }
        return REMEMBER_LOGIN_VIEW;
    }

    @PostMapping("/remember-login")
    public String rememberLogin(@NonNull Authentication authentication,
                                @NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        rememberMeServices.loginSuccess(request, response, rememberMeAuthentication(userId, authentication));
        rememberLoginPromptService.clearSkip(userId);
        return HOME_REDIRECT;
    }

    @PostMapping("/remember-login/skip")
    public String skipRememberLogin(@NonNull Authentication authentication,
                                    @NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response) {
        final UUID userId = currentUserIdResolver.requireUserId(authentication);
        rememberLoginPromptService.rememberSkip(userId);
        rememberMeServices.loginFail(request, response);
        return HOME_REDIRECT;
    }

    private Authentication rememberMeAuthentication(UUID userId, Authentication authentication) {
        final UserDetails userDetails = User.withUsername(userId.toString())
                .password("{noop}remembered")
                .authorities(authentication.getAuthorities())
                .build();
        return UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
    }
}
