package org.tubalabs.app.etc.db;

import lombok.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    @NonNull
    @Override
    protected List<?> userConverters() {
        return List.of(
                new InstantToTimestampConverter(),
                new TimestampToInstantConverter()
        );
    }

    @WritingConverter
    static class InstantToTimestampConverter implements Converter<Instant, Timestamp> {

        @Override
        public Timestamp convert(Instant source) {
            return Timestamp.from(source);
        }
    }

    @ReadingConverter
    static class TimestampToInstantConverter implements Converter<Timestamp, Instant> {

        @Override
        public Instant convert(Timestamp source) {
            return source.toInstant();
        }
    }
}