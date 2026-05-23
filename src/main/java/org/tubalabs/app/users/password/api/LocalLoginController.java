package org.tubalabs.app.users.password.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.tubalabs.app.users.password.security.LocalSessionAuthentication;
import org.tubalabs.app.users.password.LocalUserService;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class LocalLoginController {

    private final LocalUserService localUserService;
    private final LocalSessionAuthentication localSessionAuthentication;

    @PostMapping("/login/local")
    public String login(@RequestParam @NonNull String email,
                        @RequestParam @NonNull String password,
                        @NonNull HttpServletRequest request,
                        @NonNull HttpServletResponse response) {
        try {
            localUserService.login(email, password, clientIp(request), userAgent(request));
            localSessionAuthentication.authenticate(email, request, response);
            return "redirect:/";
        } catch (BadCredentialsException exception) {
            return "redirect:/login/local?error";
        }
    }

    private String clientIp(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getRemoteAddr(), "");
    }

    private String userAgent(HttpServletRequest request) {
        return Objects.requireNonNullElse(request.getHeader("User-Agent"), "");
    }
}
