package org.tubalabs.app.etc.startup.sections;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
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
@RequiredArgsConstructor
public class HttpEndpointsStartupKeyValueSection implements StartupKeyValueSection {
    private final Environment environment;
    private final WebEndpointsSupplier webEndpointsSupplier;


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
        for (ExposableWebEndpoint endpoint : webEndpointsSupplier.getEndpoints()) {
            values.put("Actuator " + endpoint.getEndpointId(), actuatorBaseUrl + "/" + endpoint.getRootPath());
        }
        values.put("Swagger UI", "http://localhost:" + port + "/swagger-ui/index.html#/");
        values.put("Local URL", "http://localhost:" + port);
        return values;
    }
}
