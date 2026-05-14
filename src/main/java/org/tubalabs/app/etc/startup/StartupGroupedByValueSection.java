package org.tubalabs.app.etc.startup;

import java.util.Comparator;

public interface StartupGroupedByValueSection extends StartupKeyValueSection {

    default Comparator<String> groupedValueComparator() {
        return String::compareTo;
    }
}
