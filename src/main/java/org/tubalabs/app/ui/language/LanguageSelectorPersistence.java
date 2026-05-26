package org.tubalabs.app.ui.language;

public enum LanguageSelectorPersistence {
    LOCAL_STORAGE("local"),
    DATABASE("db");

    private final String attributeValue;

    LanguageSelectorPersistence(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String attributeValue() {
        return attributeValue;
    }
}
