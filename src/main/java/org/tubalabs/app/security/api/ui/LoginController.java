package org.tubalabs.app.security.api.ui;

import lombok.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    private static final String LOGIN_VIEW = "org/tubalabs/app/security/login";
    private static final String LOCAL_LOGIN_VIEW = "org/tubalabs/app/security/login-local";
    private static final LoginOption LOCAL_LOGIN_OPTION =
            new LoginOption("Continue with email and password", "/login/local");

    private final List<LoginOption> loginOptions;

    public LoginController(@NonNull ClientRegistrationRepository clientRegistrationRepository) {
        this.loginOptions = loginOptions(clientRegistrationRepository);
    }

    @GetMapping("/login")
    public String login(Authentication authentication, @NonNull Model model) {
        if (authenticated(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("loginOptions", loginOptions);
        return LOGIN_VIEW;
    }

    @GetMapping("/login/local")
    public String localLogin(Authentication authentication,
                             @RequestParam @NonNull Optional<String> error,
                             @RequestParam @NonNull Optional<String> registered,
                             @NonNull Model model) {
        if (authenticated(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("hasLoginError", error.isPresent());
        model.addAttribute("registrationComplete", registered.isPresent());
        return LOCAL_LOGIN_VIEW;
    }

    private boolean authenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private List<LoginOption> loginOptions(@NonNull ClientRegistrationRepository clientRegistrationRepository) {
        final List<LoginOption> options = new ArrayList<>();
        options.addAll(oauth2LoginOptions(clientRegistrationRepository));
        options.add(LOCAL_LOGIN_OPTION);
        return List.copyOf(options);
    }

    private List<LoginOption> oauth2LoginOptions(@NonNull ClientRegistrationRepository clientRegistrationRepository) {
        if (!(clientRegistrationRepository instanceof Iterable<?> iterable)) {
            return List.of();
        }

        final List<LoginOption> options = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof ClientRegistration clientRegistration) {
                options.add(new LoginOption(
                        clientRegistration.getClientName(),
                        "/oauth2/authorization/" + clientRegistration.getRegistrationId()));
            }
        }
        return List.copyOf(options);
    }

    public record LoginOption(@NonNull String label, @NonNull String href) {
    }
}
