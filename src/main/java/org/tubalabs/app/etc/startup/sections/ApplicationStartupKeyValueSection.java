package org.tubalabs.app.etc.startup.sections;

import lombok.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.startup.ConditionalOnStartupPrinterSection;
import org.tubalabs.app.etc.startup.StartupKeyValueSection;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Order(100)
@Component
@ConditionalOnStartupPrinterSection(name = "applicationstartupkeyvaluesection.enabled")
public class ApplicationStartupKeyValueSection implements StartupKeyValueSection {
    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildProperties;

    public ApplicationStartupKeyValueSection(@NonNull Environment environment, @NonNull ObjectProvider<BuildProperties> buildProperties) {
        this.environment = environment;
        this.buildProperties = buildProperties;
    }

    @Override
    public String title() {
        return "Application";
    }

    @Override
    public Map<String, String> values() {
        final Map<String, String> values = new LinkedHashMap<>();
        final BuildProperties properties = buildProperties.getIfAvailable();
        if (properties == null) {
            values.put("Name", "missing");
            values.put("Artifact", "missing");
            values.put("Version", "missing");
            values.put("Build time", "missing");
        } else {
            values.put("Name", properties.getName());
            values.put("Artifact", properties.getGroup() + "." + properties.getArtifact());
            values.put("Version", properties.getVersion());
            values.put("Build time", String.valueOf(properties.getTime()));
        }
        values.put("Spring application", environment.getProperty("spring.application.name", ""));
        values.put("Spring Boot", SpringBootVersion.getVersion());
        values.put("Profiles", activeProfiles());
        values.put("Started at", Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()).toString());
        addIfPresent(values, "IMAGE");
        addIfPresent(values, "GCP_PROJECT");
        addIfPresent(values, "HOSTNAME");
        return values;
    }

    private String activeProfiles() {
        final String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return String.join(", ", profiles);
    }

    private void addIfPresent(Map<String, String> values, String key) {
        final String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            values.put(key, value);
        }
    }
}
