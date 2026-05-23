package org.tubalabs.app;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class MainLocalNoPersistance {

    public static void main(String[] args) {
        final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
        postgres.start();
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
        OauthTemplateApp.run(args);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down postgres");
            postgres.stop();
        }));

    }
}


