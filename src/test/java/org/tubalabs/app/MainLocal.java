package org.tubalabs.app;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainLocal {

    private static final String POSTGRES_IMAGE = "postgres:18";
    private static final String POSTGRES_CONTAINER_NAME = "oauth-template-app-postgres";
    private static final String DATABASE_HOST = "localhost";
    private static final int DATABASE_PORT = 55432;
    private static final String DATABASE_NAME = "oauth_template_app";
    private static final String DATABASE_USERNAME = "oauth_template_app";
    private static final String DATABASE_PASSWORD = "oauth_template_app";
    private static final String POSTGRES_DATA_VOLUME_NAME = "oauth-template-app-postgres-18-data";
    private static final String POSTGRES_DATA_DIRECTORY = "/var/lib/postgresql";
    private static final int POSTGRES_PORT = 5432;
    private static final Duration PORT_CHECK_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration DATABASE_STARTUP_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration DOCKER_COMMAND_TIMEOUT = Duration.ofSeconds(20);

    public static void main(String[] args) {
        ensurePostgresRunning();
        configureDatasource();
        OauthTemplateApp.run(args);
    }

    private static void ensurePostgresRunning() {
        if (isLocalPostgresRunning()) {
            System.out.println("Using Postgres on " + DATABASE_HOST + ":" + DATABASE_PORT);
            return;
        }

        if (dockerContainerExists()) {
            startDockerContainer();
        } else {
            createDockerContainer();
        }

        waitForLocalPostgres();
        System.out.println("Postgres is available on " + DATABASE_HOST + ":" + DATABASE_PORT);
        System.out.println("Docker container: " + POSTGRES_CONTAINER_NAME);
        System.out.println("Docker volume: " + POSTGRES_DATA_VOLUME_NAME);
    }

    private static boolean isLocalPostgresRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(DATABASE_HOST, DATABASE_PORT), Math.toIntExact(PORT_CHECK_TIMEOUT.toMillis()));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean dockerContainerExists() {
        final DockerCommandResult result = runDockerCommand(
                "ps", "--all", "--quiet", "--filter", "name=^/" + POSTGRES_CONTAINER_NAME + "$");
        if (result.exitCode() == 0) {
            return !result.output().isBlank();
        }
        throw new IllegalStateException("Could not inspect Docker container " + POSTGRES_CONTAINER_NAME + ": " + result.output());
    }

    private static void startDockerContainer() {
        System.out.println("Starting existing Docker container: " + POSTGRES_CONTAINER_NAME);
        runRequiredDockerCommand("start", POSTGRES_CONTAINER_NAME);
    }

    private static void createDockerContainer() {
        System.out.println("Creating Docker container: " + POSTGRES_CONTAINER_NAME);
        runRequiredDockerCommand(
                "run", "--detach",
                "--name", POSTGRES_CONTAINER_NAME,
                "--publish", DATABASE_PORT + ":" + POSTGRES_PORT,
                "--env", "POSTGRES_DB=" + DATABASE_NAME,
                "--env", "POSTGRES_USER=" + DATABASE_USERNAME,
                "--env", "POSTGRES_PASSWORD=" + DATABASE_PASSWORD,
                "--volume", POSTGRES_DATA_VOLUME_NAME + ":" + POSTGRES_DATA_DIRECTORY,
                POSTGRES_IMAGE);
    }

    private static void waitForLocalPostgres() {
        final long deadline = System.nanoTime() + DATABASE_STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (isLocalPostgresRunning()) {
                return;
            }
            sleepBriefly();
        }
        throw new IllegalStateException("Postgres did not become available on " + DATABASE_HOST + ":" + DATABASE_PORT);
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(250);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Postgres", exception);
        }
    }

    private static void runRequiredDockerCommand(String... arguments) {
        final DockerCommandResult result = runDockerCommand(arguments);
        if (result.exitCode() != 0) {
            throw new IllegalStateException("Docker command failed: docker " + String.join(" ", arguments) + "\n" + result.output());
        }
    }

    private static DockerCommandResult runDockerCommand(String... arguments) {
        final List<String> command = new ArrayList<>();
        command.add("docker");
        command.addAll(Arrays.asList(arguments));

        try {
            final Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            final boolean completed = process.waitFor(DOCKER_COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new DockerCommandResult(1, "Docker command timed out: docker " + String.join(" ", arguments));
            }
            final String output = readOutput(process.getInputStream());
            return new DockerCommandResult(process.exitValue(), output.trim());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not run Docker command. Is Docker Desktop running?", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running Docker command", exception);
        }
    }

    private static String readOutput(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void configureDatasource() {
        System.setProperty("spring.datasource.url", jdbcUrl());
        System.setProperty("spring.datasource.username", DATABASE_USERNAME);
        System.setProperty("spring.datasource.password", DATABASE_PASSWORD);
    }

    private static String jdbcUrl() {
        return "jdbc:postgresql://" + DATABASE_HOST + ":" + DATABASE_PORT + "/" + DATABASE_NAME + "?loggerLevel=OFF";
    }

    private record DockerCommandResult(int exitCode, String output) {
    }
}
