package org.tubalabs.app.ui.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.tubalabs.app.users.identity.password.security.LocalSessionAuthentication;
import org.tubalabs.app.users.LoginResult;
import org.tubalabs.app.users.current.CurrentUserSession;
import org.tubalabs.app.users.identity.password.LocalUserService;
import org.tubalabs.app.users.profile.ProfileSetupRequirementService;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class LocalLoginController {

    private final LocalUserService localUserService;
    private final LocalSessionAuthentication localSessionAuthentication;
    private final ProfileSetupRequirementService profileSetupRequirementService;
    private final CurrentUserSession currentUserSession;

    @PostMapping("/login/local")
    public String login(@RequestParam @NonNull String email,
                        @RequestParam @NonNull String password,
                        @NonNull HttpServletRequest request,
                        @NonNull HttpServletResponse response,
                        @NonNull RedirectAttributes redirectAttributes) {
        try {
            final LoginResult result = localUserService.login(email, password, clientIp(request), userAgent(request));
            localSessionAuthentication.authenticate(email, request, response);
            final boolean profileSetupRequired =
                    profileSetupRequirementService.requireSetupIfProfileIncomplete(request, result.userId());
            currentUserSession.refresh(request, result.userId(), profileSetupRequired);
            if (profileSetupRequired) {
                return "redirect:/profile";
            }
            return "redirect:/remember-login";
        } catch (BadCredentialsException exception) {
            redirectAttributes.addAttribute("error", true);
            redirectAttributes.addAttribute("email", email);
            return "redirect:/login/local";
        }
    }

    private String clientIp(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }
}
