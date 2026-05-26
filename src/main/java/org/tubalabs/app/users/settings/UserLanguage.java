package org.tubalabs.app.users.settings;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import lombok.NonNull;
import org.tubalabs.app.localization.LocalizationKey;

public enum UserLanguage implements LocalizationKey {
    ENGLISH("en", "language.option.en", "\uD83C\uDDEC\uD83C\uDDE7"),
    NORWEGIAN("nb", "language.option.nb", "\uD83C\uDDF3\uD83C\uDDF4");

    public static final UserLanguage DEFAULT = ENGLISH;

    private final String tag;
    private final String localizationKey;
    private final String flag;

    UserLanguage(@NonNull String tag, @NonNull String localizationKey, @NonNull String flag) {
        this.tag = tag;
        this.localizationKey = localizationKey;
        this.flag = flag;
    }

    public String tag() {
        return tag;
    }

    @Override
    public String getLocalizationKey() {
        return localizationKey;
    }

    public String flag() {
        return flag;
    }

    public Locale locale() {
        return Locale.forLanguageTag(tag);
    }

    public static List<UserLanguage> supportedLanguages() {
        return List.copyOf(Arrays.asList(values()));
    }

    public static Optional<UserLanguage> fromTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return Optional.empty();
        }

        final String normalizedTag = tag.trim().toLowerCase(Locale.ROOT);
        return supportedLanguages()
                .stream()
                .filter(language -> language.tag.equals(normalizedTag))
                .findFirst();
    }

    public static String defaultTag() {
        return DEFAULT.tag();
    }

    public static String normalizedTagOrDefault(String tag) {
        return fromTag(tag)
                .orElse(DEFAULT)
                .tag();
    }

    public static UserLanguage fromTagOrDefault(String tag) {
        return fromTag(tag).orElse(DEFAULT);
    }
}
