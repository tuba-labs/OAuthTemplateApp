package org.tubalabs.app.etc.startup;

import java.util.Map;

public interface StartupKeyValueSection {
    String title();

    Map<String, String> values();
}
