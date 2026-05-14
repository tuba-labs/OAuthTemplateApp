package org.tubalabs.app.etc.startup.sections;

import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.startup.ConditionalOnStartupPrinterSection;
import org.tubalabs.app.etc.startup.StartupKeyValueSection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Order(400)
@Component
@ConditionalOnBean(DataSource.class)
@ConditionalOnStartupPrinterSection(name = "databasestartupkeyvaluesection.enabled")
public class DatabaseStartupKeyValueSection implements StartupKeyValueSection {
    private final DataSource dataSource;

    public DatabaseStartupKeyValueSection(@NonNull DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String title() {
        return "Database connection";
    }

    @Override
    public Map<String, String> values() {
        final Map<String, String> values = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            final DatabaseMetaData metadata = connection.getMetaData();
            String url = metadata.getURL();
            final int queryIndex = url.indexOf('?');
            if (queryIndex >= 0) {
                url = url.substring(0, queryIndex);
            }
            values.put("Database", metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion());
            values.put("URL", url);
            values.put("Schema", connection.getSchema());
            values.put("Catalog", connection.getCatalog());
            values.put("Driver", metadata.getDriverName() + " v" + metadata.getDriverVersion());
            addTimeInfo(values, connection);
            addDataSourceInfo(values);
        } catch (Exception exception) {
            log.debug("Unable to determine DB details", exception);
        }
        return values;
    }

    private void addDataSourceInfo(Map<String, String> values) {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            values.put("Pool", hikariDataSource.getPoolName());
            values.put("Max pool size", String.valueOf(hikariDataSource.getMaximumPoolSize()));
        }
    }

    private void addTimeInfo(Map<String, String> values, Connection connection) {
        final Instant javaNow = Instant.now();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("SHOW TIME ZONE")) {
                if (resultSet.next()) {
                    values.put("Timezone", resultSet.getString(1));
                }
            } catch (Exception exception) {
                log.debug("Failed to load db Timezone details", exception);
            }
            try (ResultSet resultSet = statement.executeQuery("SELECT CURRENT_TIMESTAMP")) {
                if (resultSet.next()) {
                    final Instant dbNow = resultSet.getTimestamp(1).toInstant();
                    final long driftSeconds = Duration.between(dbNow, javaNow).getSeconds();
                    values.put("DB now (UTC)", dbNow.toString());
                    values.put("Clock drift", driftSeconds + " s");
                }
            } catch (Exception exception) {
                log.debug("Failed to load db time", exception);
            }
        } catch (Exception exception) {
            log.debug("Failed to create statement", exception);
        }
    }
}
