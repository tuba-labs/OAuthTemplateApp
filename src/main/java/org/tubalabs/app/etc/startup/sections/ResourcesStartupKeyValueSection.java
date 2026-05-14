package org.tubalabs.app.etc.startup.sections;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.startup.ConditionalOnStartupPrinterSection;
import org.tubalabs.app.etc.startup.StartupKeyValueSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Order(300)
@Component
@ConditionalOnStartupPrinterSection(name = "resourcesstartupkeyvaluesection.enabled")
public class ResourcesStartupKeyValueSection implements StartupKeyValueSection {

    @Override
    public String title() {
        return "Resources";
    }

    @Override
    public Map<String, String> values() {
        final Runtime runtime = Runtime.getRuntime();
        final Map<String, String> values = new LinkedHashMap<>();
        values.put("CPU cores", String.valueOf(runtime.availableProcessors()));
        values.put("Max heap", (runtime.maxMemory() / (1024 * 1024)) + " MB");
        values.put("Total heap", (runtime.totalMemory() / (1024 * 1024)) + " MB");
        for (File root : File.listRoots()) {
            values.put(root.getAbsolutePath(), "Writable=" + root.canWrite() + ", FreeGB=" + root.getFreeSpace() / (1024 * 1024 * 1024));
        }
        addMountDetails(values);
        return values;
    }

    private void addMountDetails(Map<String, String> values) {
        final Path mountPath = Paths.get("/mnt");
        if (!Files.exists(mountPath)) {
            return;
        }
        try (Stream<Path> paths = Files.list(mountPath)) {
            paths.forEach(path -> {
                final File file = path.toFile();
                values.put(path.toString(), "Writable=" + file.canWrite() + ", FreeGB=" + file.getFreeSpace() / (1024 * 1024 * 1024));
            });
        } catch (Exception exception) {
            log.debug("Unable to determine path details for {} ", mountPath, exception);
        }
    }
}
