package org.tubalabs.app.localization;

import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalizationServiceTest {

    private static final Locale TEST_LOCALE = Locale.ENGLISH;
    private static final String MESSAGE_KEY = "test.message";
    private static final String MESSAGE = "Translated message";
    private static final String ARGUMENT_MESSAGE_KEY = "test.argument-message";
    private static final String ARGUMENT_MESSAGE = "Hello Person";

    private final StaticMessageSource messageSource = new StaticMessageSource();
    private final LocalizationService localizationService = new LocalizationService(messageSource);

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(TEST_LOCALE);
        messageSource.addMessage(MESSAGE_KEY, TEST_LOCALE, MESSAGE);
        messageSource.addMessage(ARGUMENT_MESSAGE_KEY, TEST_LOCALE, "Hello {0}");
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resolvesStringKey() {
        assertThat(localizationService.message(MESSAGE_KEY)).isEqualTo(MESSAGE);
    }

    @Test
    void resolvesEnumKey() {
        assertThat(localizationService.message(TestLocalizationKey.MESSAGE)).isEqualTo(MESSAGE);
    }

    @Test
    void resolvesArguments() {
        assertThat(localizationService.message(ARGUMENT_MESSAGE_KEY, "Person")).isEqualTo(ARGUMENT_MESSAGE);
    }

    @Test
    void throwsWhenKeyIsMissing() {
        assertThatThrownBy(() -> localizationService.message("test.missing"))
                .isInstanceOf(NoSuchMessageException.class);
    }

    private enum TestLocalizationKey implements LocalizationKey {
        MESSAGE(MESSAGE_KEY);

        private final String localizationKey;

        TestLocalizationKey(@NonNull String localizationKey) {
            this.localizationKey = localizationKey;
        }

        @Override
        public String getLocalizationKey() {
            return localizationKey;
        }
    }
}
