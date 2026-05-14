package org.tubalabs.app.etc.startup.sections;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.startup.ConditionalOnStartupPrinterSection;
import org.tubalabs.app.etc.startup.StartupGroupedByValueSection;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Order(500)
@Component
@ConditionalOnBean(LoggingSystem.class)
@ConditionalOnStartupPrinterSection(name = "loggingstartupgroupedbyvaluesection.enabled")
public class LoggingStartupGroupedByValueSection implements StartupGroupedByValueSection {
    private final LoggingSystem loggingSystem;

    public LoggingStartupGroupedByValueSection(@NonNull LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
    }

    @Override
    public String title() {
        return "Effective logging";
    }

    @Override
    public Comparator<String> groupedValueComparator() {
        return Comparator.comparingInt(this::levelOrder).thenComparing(String::compareTo);
    }

    @Override
    public Map<String, String> values() {
        final Map<String, String> values = new LinkedHashMap<>();
        final Logger random = LoggerFactory.getLogger(UUID.randomUUID().toString().replace("-", ""));
        final LoggerConfiguration cfg = loggingSystem.getLoggerConfiguration(random.getName());
        loggingSystem.getLoggerConfigurations().stream()
                .filter(configuration -> configuration.getConfiguredLevel() != null)
                .forEach(configuration -> values.put(configuration.getName(), String.valueOf(configuration.getConfiguredLevel())));
        addEffectiveRootWarning(values, cfg);
        return values;
    }

    private void addEffectiveRootWarning(Map<String, String> values, LoggerConfiguration effectiveConfig) {
        final LoggerConfiguration rootConfig = loggingSystem.getLoggerConfiguration("ROOT");
        if (effectiveConfig == null || rootConfig == null || rootConfig.getConfiguredLevel() == null) {
            return;
        }
        final String effectiveLevel = String.valueOf(effectiveConfig.getEffectiveLevel());
        final String configuredLevel = String.valueOf(rootConfig.getConfiguredLevel());
        if (!effectiveLevel.equals(configuredLevel)) {
            values.put("Unexpected inherited logger level: ROOT=" + configuredLevel + ", effective=" + effectiveLevel, "WARN");
        }
    }

    private int levelOrder(String level) {
        return switch (level) {
            case "OFF" -> 0;
            case "ERROR" -> 1;
            case "WARN" -> 2;
            case "INFO" -> 3;
            case "DEBUG" -> 4;
            case "TRACE" -> 5;
            default -> 6;
        };
    }
}
