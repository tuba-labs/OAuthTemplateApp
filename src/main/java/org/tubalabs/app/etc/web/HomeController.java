package org.tubalabs.app.etc.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal final OAuth2User user) {
        if (user == null) {
            return "Not logged in. " +
                    "<p/> " +
                    "<p> Try: <a href=\"/login\">Login</a></p>";
        }
        final String name =
                user.getAttribute("email") != null
                        ? user.getAttribute("email")
                        : user.getName();
        return "Logged in as " + name +
                "<p/> " +
                "<p> Logout: <a href=\"/logout\">Login</a></p>";

    }
}