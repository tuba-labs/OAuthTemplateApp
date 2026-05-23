package org.tubalabs.app.users.password.validation.vetoers.password;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PwnedPasswordClientTest {

    private static final String PASSWORD = "password";
    private static final String PASSWORD_HASH_PREFIX = "5BAA6";
    private static final String PASSWORD_HASH_SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";
    private static final String OTHER_SUFFIX = "00000000000000000000000000000000000";
    private static final String ADD_PADDING_HEADER = "Add-Padding";
    private static final String ADD_PADDING_VALUE = "true";
    private static final int OK_STATUS = 200;
    private static final int ERROR_STATUS = 500;

    private final AtomicReference<String> requestedPath = new AtomicReference<>();
    private final AtomicReference<String> addPaddingHeader = new AtomicReference<>();

    private HttpServer server;
    private String responseBody;
    private int responseStatus;

    @BeforeEach
    void startServer() throws IOException {
        responseBody = "";
        responseStatus = OK_STATUS;
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/range/" + PASSWORD_HASH_PREFIX, this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsTrueWhenSuffixHasPositiveCount() {
        responseBody = OTHER_SUFFIX + ":0\n" + PASSWORD_HASH_SUFFIX + ":42\n";

        final PwnedPasswordClient client = newClient(true);

        assertThat(client.isPwned(PASSWORD)).isTrue();
        assertThat(requestedPath.get()).isEqualTo("/range/" + PASSWORD_HASH_PREFIX);
        assertThat(addPaddingHeader.get()).isEqualTo(ADD_PADDING_VALUE);
    }

    @Test
    void returnsFalseWhenSuffixIsMissing() {
        responseBody = OTHER_SUFFIX + ":42\n";

        final PwnedPasswordClient client = newClient(true);

        assertThat(client.isPwned(PASSWORD)).isFalse();
    }

    @Test
    void returnsFalseWhenClientIsDisabled() {
        final PwnedPasswordClient client = newClient(false);

        assertThat(client.isPwned(PASSWORD)).isFalse();
        assertThat(requestedPath.get()).isNull();
    }

    @Test
    void returnsFalseWhenServiceFails() {
        responseStatus = ERROR_STATUS;

        final PwnedPasswordClient client = newClient(true);

        assertThat(client.isPwned(PASSWORD)).isFalse();
    }

    private PwnedPasswordClient newClient(boolean enabled) {
        return new PwnedPasswordClient(RestClient.builder(), baseUrl(), enabled);
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange) throws IOException {
        requestedPath.set(exchange.getRequestURI().getPath());
        addPaddingHeader.set(exchange.getRequestHeaders().getFirst(ADD_PADDING_HEADER));

        final byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(responseStatus, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
