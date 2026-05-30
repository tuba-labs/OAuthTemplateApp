package org.tubalabs.app.testtools;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class AbstractJdbcTestBaseTestClass {

    private static final String HIKARI_MAXIMUM_POOL_SIZE_PROPERTY =
            "org.tubalabs.app.testtools.hikari.maximum-pool-size";
    private static final String DEFAULT_HIKARI_MAXIMUM_POOL_SIZE = "2";

    static final PostgreSQLContainer POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
        POSTGRES.start();
        Runtime.getRuntime().addShutdownHook(new Thread(POSTGRES::stop));
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", AbstractJdbcTestBaseTestClass::hikariMaximumPoolSize);
        registry.add("spring.datasource.hikari.minimum-idle", () -> "0");
    }

    private static String hikariMaximumPoolSize() {
        return System.getProperty(HIKARI_MAXIMUM_POOL_SIZE_PROPERTY, DEFAULT_HIKARI_MAXIMUM_POOL_SIZE);
    }

    protected static void overrideHikariMaximumPoolSize(int maximumPoolSize) {
        System.setProperty(HIKARI_MAXIMUM_POOL_SIZE_PROPERTY, String.valueOf(maximumPoolSize));
    }

    protected static void clearHikariMaximumPoolSizeOverride() {
        System.clearProperty(HIKARI_MAXIMUM_POOL_SIZE_PROPERTY);
    }
}
