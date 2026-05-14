package org.tubalabs.app;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class MainLocal {

    public static void main(String[] args) {
        final PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
        /*.withFileSystemBind("C:/pgdata", "/var/lib/postgresql/data")*/
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
