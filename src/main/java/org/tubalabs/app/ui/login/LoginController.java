package org.tubalabs.app.ui.login;

import lombok.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.ui.login.dtos.LoginOptionDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    private static final String LOGIN_VIEW = "ui/login/login";
    private static final String LOCAL_LOGIN_VIEW = "ui/login/login-local";
    private static final String LOCAL_LOGIN_PATH = "/login/local";

    private final List<LoginOptionDto> oauth2LoginOptions;
    private final LocalizationService localizationService;

    public LoginController(@NonNull ClientRegistrationRepository clientRegistrationRepository,
                           @NonNull LocalizationService localizationService) {
        this.oauth2LoginOptions = oauth2LoginOptions(clientRegistrationRepository);
        this.localizationService = localizationService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication, @NonNull Model model) {
        if (authenticated(authentication)) {
            return "redirect:/";
        }
        model.addAttribute("loginOptions", loginOptions());
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

    private List<LoginOptionDto> loginOptions() {
        final List<LoginOptionDto> options = new ArrayList<>();
        options.addAll(oauth2LoginOptions);
        options.add(new LoginOptionDto(localizationService.message("login.option.local"), LOCAL_LOGIN_PATH));
        return List.copyOf(options);
    }

    private List<LoginOptionDto> oauth2LoginOptions(@NonNull ClientRegistrationRepository clientRegistrationRepository) {
        if (!(clientRegistrationRepository instanceof Iterable<?> iterable)) {
            return List.of();
        }

        final List<LoginOptionDto> options = new ArrayList<>();
        for (Object item : iterable) {
            if (item instanceof ClientRegistration clientRegistration) {
                options.add(new LoginOptionDto(
                        clientRegistration.getClientName(),
                        "/oauth2/authorization/" + clientRegistration.getRegistrationId()));
            }
        }
        return List.copyOf(options);
    }
}
