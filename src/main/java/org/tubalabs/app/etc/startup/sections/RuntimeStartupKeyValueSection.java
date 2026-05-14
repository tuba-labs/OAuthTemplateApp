package org.tubalabs.app.etc.startup.sections;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tubalabs.app.etc.startup.ConditionalOnStartupPrinterSection;
import org.tubalabs.app.etc.startup.StartupKeyValueSection;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Order(200)
@Component
@ConditionalOnStartupPrinterSection(name = "runtimestartupkeyvaluesection.enabled")
public class RuntimeStartupKeyValueSection implements StartupKeyValueSection {

    @Override
    public String title() {
        return "Runtime";
    }

    @Override
    public Map<String, String> values() {
        final Map<String, String> values = new LinkedHashMap<>();
        final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        values.put("PID", String.valueOf(runtime.getPid()));
        values.put("Uptime", Duration.ofMillis(runtime.getUptime()).toString());
        values.put("Java", System.getProperty("java.version"));
        values.put("JVM", System.getProperty("java.vm.name"));
        values.put("Java vendor", System.getProperty("java.vendor"));
        values.put("Assertions", assertionsEnabled() ? "enabled" : "disabled");
        values.put("OS", os.getName());
        values.put("OS version", os.getVersion());
        values.put("Architecture", os.getArch());
        values.put("OS user", System.getProperty("user.name"));
        values.put("Timezone", ZoneId.systemDefault().toString());
        values.put("JVM timezone property", System.getProperty("user.timezone"));
        values.put("Locale", Locale.getDefault().toLanguageTag());
        values.put("File encoding", Charset.defaultCharset().displayName());
        values.put("Temp dir", System.getProperty("java.io.tmpdir"));
        return values;
    }

    private boolean assertionsEnabled() {
        boolean enabled = false;
        assert enabled = true;
        return enabled;
    }
}
