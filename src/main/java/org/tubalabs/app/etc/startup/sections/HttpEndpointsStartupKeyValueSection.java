package org.tubalabs.app.etc.startup.sections;

import lombok.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.startup.ConditionalOnStartupPrinterSection;
import org.tubalabs.app.etc.startup.StartupKeyValueSection;

import java.util.LinkedHashMap;
import java.util.Map;

@Order(600)
@Component
@ConditionalOnStartupPrinterSection(name = "httpendpointsstartupkeyvaluesection.enabled")
public class HttpEndpointsStartupKeyValueSection implements StartupKeyValueSection {
    private final Environment environment;

    public HttpEndpointsStartupKeyValueSection(@NonNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public String title() {
        return "HTTP endpoints";
    }

    @Override
    public Map<String, String> values() {
        final int port = environment.getProperty("local.server.port", Integer.class, 0);
        final int managementPort = environment.getProperty("local.management.port", Integer.class, port);
        final String actuatorBasePath = environment.getProperty("management.endpoints.web.base-path", "/actuator");
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("Port", String.valueOf(port));
        if (managementPort != port) {
            values.put("Management port", String.valueOf(managementPort));
        }

        final String actuatorBaseUrl = "http://localhost:" + managementPort + actuatorBasePath;
        values.put("Actuator URL", actuatorBaseUrl);
        values.put("Actuator health", actuatorBaseUrl + "/health");
        values.put("Actuator info", actuatorBaseUrl + "/info");
        values.put("Actuator loggers", actuatorBaseUrl + "/loggers");
        values.put("Actuator metrics", actuatorBaseUrl + "/metrics");
        values.put("Actuator startup", actuatorBaseUrl + "/startup");
        values.put("Startup metric", actuatorBaseUrl + "/metrics/application.starts");
        values.put("Local URL", "http://localhost:" + port);
        values.put("Swagger UI", "http://localhost:" + port + "/swagger-ui/index.html#/");
        return values;
    }
}
