package org.tubalabs.app.startpage.api.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final String HOME_VIEW = "org/tubalabs/app/startpage/home";

    @GetMapping("/")
    public String home() {
        return HOME_VIEW;
    }
}
