package org.tubalabs.app.etc.startup;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Component
@Slf4j
public class StartupPrinter {
    public static final int INDENT_WIDTH = 2;
    private final List<StartupKeyValueSection> sections;

    public StartupPrinter(@NonNull List<StartupKeyValueSection> sections) {
        this.sections = List.copyOf(sections);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printStartupMessage() {
        final StringBuilder out = new StringBuilder(1024);
        out.append("\n------------------------------------------------------------\n");
        out.append("Application started\n");
        sections.forEach(section -> renderSection(out, section));
        out.append("------------------------------------------------------------");
        log.info(out.toString());
    }

    private void renderSection(StringBuilder out, StartupKeyValueSection section) {
        out.append("\n").repeat(" ", INDENT_WIDTH * 1).append(section.title()).append(":\n");
        final Map<String, String> values = section.values();
        if (section instanceof StartupGroupedByValueSection groupedValueSection) {
            renderGroupedValues(out, groupedValueSection, values);
            return;
        }
        renderKeyValues(out, values);
    }

    private void renderKeyValues(StringBuilder out, Map<String, String> values) {
        final int keyWidth = values.keySet().stream()
                .mapToInt(String::length)
                .max()
                .orElse(0);
        values.forEach((key, value) -> out.repeat(" ", INDENT_WIDTH * 2)
                .append(key)
                .repeat(" ", keyWidth - key.length())
                .append(" : ")
                .append(value)
                .append('\n'));
    }

    private void renderGroupedValues(StringBuilder out, StartupGroupedByValueSection section, Map<String, String> values) {
        final Map<String, Set<String>> keysByValue = new TreeMap<>(section.groupedValueComparator());
        values.forEach((key, value) -> keysByValue.computeIfAbsent(value, ignored -> new TreeSet<>()).add(key));
        keysByValue.forEach((value, keys) -> {
            out.repeat(" ", INDENT_WIDTH * 2).append(value).append(":\n");
            keys.forEach(key -> out.repeat(" ", INDENT_WIDTH * 3).append(key).append('\n'));
        });
    }

}
