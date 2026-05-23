package org.tubalabs.app.etc.startup;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupMetrics {
    private final Counter startupCounter;

    public StartupMetrics(MeterRegistry meterRegistry) {
        this.startupCounter = Counter.builder("application.starts")
                .description("Number of completed application startups")
                .register(meterRegistry);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void countStartup() {
        startupCounter.increment();
    }
}
