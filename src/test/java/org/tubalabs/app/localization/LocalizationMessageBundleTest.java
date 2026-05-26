package org.tubalabs.app.localization;

import org.junit.jupiter.api.Test;
import org.tubalabs.app.users.settings.UserLanguage;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizationMessageBundleTest {

    private static final Path MAIN_JAVA_DIRECTORY = Paths.get("src/main/java");
    private static final Path MAIN_RESOURCES_DIRECTORY = Paths.get("src/main/resources");
    private static final Path TEMPLATES_DIRECTORY = MAIN_RESOURCES_DIRECTORY.resolve("templates");
    private static final String DEFAULT_BUNDLE_FILE_NAME = "messages.properties";
    private static final String BUNDLE_FILE_PREFIX = "messages";
    private static final String BUNDLE_FILE_SUFFIX = ".properties";
    private static final String JAVA_FILE_SUFFIX = ".java";
    private static final String HTML_FILE_SUFFIX = ".html";
    private static final String LOCALIZATION_KEY_INTERFACE = "implements LocalizationKey";
    private static final Pattern JAVA_LOCALIZATION_KEY_LITERAL_PATTERN =
            Pattern.compile("\"\\{?([a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)+)\\}?\"");
    private static final Pattern JAVA_BRACED_LOCALIZATION_KEY_PATTERN =
            Pattern.compile("\"\\{([a-z][a-z0-9-]*(?:\\.[a-z0-9-]+)+)}\"");
    private static final Pattern LOCALIZATION_SERVICE_KEY_PATTERN =
            Pattern.compile("localizationService\\.message\\(\\s*\"([^\"]+)\"");
    private static final Pattern PAGE_TEXT_KEY_PATTERN =
            Pattern.compile("new\\s+PageText\\(\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]+)\"", Pattern.DOTALL);
    private static final Pattern THYMELEAF_LOCALIZATION_KEY_PATTERN =
            Pattern.compile("#\\{\\s*([^}\\(\\s]+)");
    private static final Pattern THYMELEAF_MESSAGE_FUNCTION_KEY_PATTERN =
            Pattern.compile("#messages\\.msg\\(\\s*'([^']+)'");

    @Test
    void messageBundleFilesMatchSupportedLanguages() throws IOException {
        final Set<String> expectedBundleFileNames = UserLanguage.supportedLanguages()
                .stream()
                .map(LocalizationMessageBundleTest::bundleFileName)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(messageBundleFileNames()).containsExactlyElementsOf(expectedBundleFileNames);
    }

    @Test
    void supportedLanguageBundlesHaveSameLocalizationKeys() throws IOException {
        final Set<String> defaultKeys = loadMessages(DEFAULT_BUNDLE_FILE_NAME).stringPropertyNames();

        for (UserLanguage language : UserLanguage.supportedLanguages()) {
            final String bundleFileName = bundleFileName(language);
            assertThat(loadMessages(bundleFileName).stringPropertyNames())
                    .as("Localization keys in %s", bundleFileName)
                    .containsExactlyInAnyOrderElementsOf(defaultKeys);
        }
    }

    @Test
    void translatedValuesArePresentForNonEmptyDefaultMessages() throws IOException {
        final Properties defaultMessages = loadMessages(DEFAULT_BUNDLE_FILE_NAME);

        for (UserLanguage language : UserLanguage.supportedLanguages()) {
            if (language == UserLanguage.DEFAULT) {
                continue;
            }
            final String bundleFileName = bundleFileName(language);
            final Properties localizedMessages = loadMessages(bundleFileName);
            final Set<String> missingTranslations = defaultMessages.stringPropertyNames()
                    .stream()
                    .filter(key -> !defaultMessages.getProperty(key).isBlank())
                    .filter(key -> localizedMessages.getProperty(key, "").isBlank())
                    .collect(Collectors.toCollection(TreeSet::new));

            assertThat(missingTranslations)
                    .as("Missing translations in %s", bundleFileName)
                    .isEmpty();
        }
    }

    @Test
    void referencedLocalizationKeysExistInDefaultBundle() throws IOException {
        final Set<String> defaultKeys = loadMessages(DEFAULT_BUNDLE_FILE_NAME).stringPropertyNames();
        final Set<String> missingKeys = referencedLocalizationKeys()
                .stream()
                .filter(key -> !defaultKeys.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(missingKeys).as("Localization keys referenced by code/templates but missing from default bundle")
                .isEmpty();
    }

    private static Set<String> messageBundleFileNames() throws IOException {
        try (Stream<Path> paths = Files.list(MAIN_RESOURCES_DIRECTORY)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.startsWith(BUNDLE_FILE_PREFIX))
                    .filter(fileName -> fileName.endsWith(BUNDLE_FILE_SUFFIX))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> referencedLocalizationKeys() throws IOException {
        final Set<String> keys = new TreeSet<>();
        for (Path javaFile : regularFiles(MAIN_JAVA_DIRECTORY, JAVA_FILE_SUFFIX)) {
            final String content = Files.readString(javaFile, StandardCharsets.UTF_8);
            keys.addAll(matches(content, LOCALIZATION_SERVICE_KEY_PATTERN));
            keys.addAll(matches(content, JAVA_BRACED_LOCALIZATION_KEY_PATTERN));
            keys.addAll(pageTextMatches(content));
            if (content.contains(LOCALIZATION_KEY_INTERFACE)) {
                keys.addAll(matches(content, JAVA_LOCALIZATION_KEY_LITERAL_PATTERN));
            }
        }
        for (Path templateFile : regularFiles(TEMPLATES_DIRECTORY, HTML_FILE_SUFFIX)) {
            final String content = Files.readString(templateFile, StandardCharsets.UTF_8);
            keys.addAll(matches(content, THYMELEAF_LOCALIZATION_KEY_PATTERN));
            keys.addAll(matches(content, THYMELEAF_MESSAGE_FUNCTION_KEY_PATTERN));
        }
        return keys;
    }

    private static List<Path> regularFiles(Path rootDirectory, String fileSuffix) throws IOException {
        try (Stream<Path> paths = Files.walk(rootDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(fileSuffix))
                    .toList();
        }
    }

    private static Set<String> matches(String content, Pattern pattern) {
        final Set<String> matches = new TreeSet<>();
        final Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        return matches;
    }

    private static Set<String> pageTextMatches(String content) {
        final Set<String> matches = new TreeSet<>();
        final Matcher matcher = PAGE_TEXT_KEY_PATTERN.matcher(content);
        while (matcher.find()) {
            matches.add(matcher.group(1));
            matches.add(matcher.group(2));
        }
        return matches;
    }

    private static Properties loadMessages(String fileName) throws IOException {
        final Path path = MAIN_RESOURCES_DIRECTORY.resolve(fileName);
        assertThat(path).as("Message bundle %s", fileName).exists();

        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    private static String bundleFileName(UserLanguage language) {
        if (language == UserLanguage.DEFAULT) {
            return DEFAULT_BUNDLE_FILE_NAME;
        }
        return BUNDLE_FILE_PREFIX + "_" + language.tag() + BUNDLE_FILE_SUFFIX;
    }
}
