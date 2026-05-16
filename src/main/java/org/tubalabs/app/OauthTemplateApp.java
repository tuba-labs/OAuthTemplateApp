package org.tubalabs.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class OauthTemplateApp {

    public static void main(String[] args) {
        run(args);
    }

    public static void run(String[] args) {
        final SpringApplication application = new SpringApplication(OauthTemplateApp.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
    }

}
