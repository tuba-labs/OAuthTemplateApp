package org.tubalabs.app.ui.rememberlogin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.tubalabs.app.security.remember.RememberedLoginName;
import org.tubalabs.app.security.remember.RememberLoginPromptService;
import org.tubalabs.app.users.identity.CurrentLoginIdentityResolver;
import org.tubalabs.app.users.identity.db.UserIdentityDbo;

@Controller
@RequiredArgsConstructor
public class RememberLoginController {

    private static final String REMEMBER_LOGIN_VIEW = "ui/rememberlogin/remember-login";
    private static final String HOME_REDIRECT = "redirect:/";

    private final CurrentLoginIdentityResolver currentLoginIdentityResolver;
    private final RememberLoginPromptService rememberLoginPromptService;
    private final RememberMeServices rememberMeServices;

    @GetMapping("/remember-login")
    public String rememberLogin(@NonNull Authentication authentication,
                                @NonNull HttpServletRequest request) {
        if (authentication instanceof RememberMeAuthenticationToken) {
            return HOME_REDIRECT;
        }
        requireCurrentIdentity(authentication);
        if (!rememberLoginPromptService.shouldAsk(request)) {
            return HOME_REDIRECT;
        }
        return REMEMBER_LOGIN_VIEW;
    }

    @PostMapping("/remember-login")
    public String rememberLogin(@NonNull Authentication authentication,
                                @NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response) {
        final UserIdentityDbo identity = requireCurrentIdentity(authentication);
        rememberMeServices.loginSuccess(request, response, rememberMeAuthentication(identity, authentication));
        rememberLoginPromptService.clearSkip(request, response);
        return HOME_REDIRECT;
    }

    @PostMapping("/remember-login/skip")
    public String skipRememberLogin(@NonNull Authentication authentication,
                                    @NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response) {
        requireCurrentIdentity(authentication);
        rememberLoginPromptService.rememberSkip(request, response);
        rememberMeServices.loginFail(request, response);
        return HOME_REDIRECT;
    }

    private Authentication rememberMeAuthentication(UserIdentityDbo identity,
                                                    Authentication authentication) {
        final UserDetails userDetails = User.withUsername(
                        RememberedLoginName.username(identity.id()))
                .password("{noop}remembered")
                .authorities(authentication.getAuthorities())
                .build();
        return UsernamePasswordAuthenticationToken.authenticated(userDetails, null, userDetails.getAuthorities());
    }

    private UserIdentityDbo requireCurrentIdentity(Authentication authentication) {
        return currentLoginIdentityResolver.identity(authentication)
                .orElseThrow(() -> new AccessDeniedException("Current login identity not found"));
    }
}
