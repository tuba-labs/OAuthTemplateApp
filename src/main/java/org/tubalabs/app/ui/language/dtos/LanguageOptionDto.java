package org.tubalabs.app.ui.language.dtos;

import lombok.NonNull;

public record LanguageOptionDto(
        @NonNull String languageTag,
        @NonNull String flag,
        @NonNull String label) {

    public String displayLabel() {
        return flag + " " + label;
    }
}
