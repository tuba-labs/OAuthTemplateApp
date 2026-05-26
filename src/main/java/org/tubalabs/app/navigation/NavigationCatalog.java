package org.tubalabs.app.navigation;

import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class NavigationCatalog {

    private static final Comparator<NavigationPageRegistration> REGISTRATION_ORDER =
            Comparator.comparingInt(NavigationPageRegistration::order)
                    .thenComparing(registration -> registration.model().relativeUrl());

    private final List<MainPage> mainPages;

    public NavigationCatalog(@NonNull List<NavigationPageRegistration> registrations) {
        this.mainPages = buildMainPages(registrations);
    }

    public List<MainPage> mainPages() {
        return mainPages;
    }

    public Optional<NavigablePage> activePage(@NonNull String currentPath) {
        return mainPages.stream()
                .flatMap(this::pageAndSubPages)
                .filter(page -> matches(currentPath, page.relativeUrl()))
                .max(Comparator.comparingInt(page -> page.relativeUrl().length()));
    }

    private List<MainPage> buildMainPages(@NonNull List<NavigationPageRegistration> registrations) {
        final Map<String, NavigationPageRegistration> registrationsByUrl = registrationsByUrl(registrations);
        final Map<String, List<NavigationPageRegistration>> childrenByParentUrl =
                childrenByParentUrl(registrations, registrationsByUrl);

        final List<MainPage> mainPages = registrations.stream()
                .filter(registration -> registration.parentRelativeUrl().isEmpty())
                .sorted(REGISTRATION_ORDER)
                .map(registration -> new MainPage(
                        registration.model(),
                        subPages(registration.model().relativeUrl(), childrenByParentUrl)))
                .toList();
        validateReachablePages(mainPages, registrations);
        return mainPages;
    }

    private Map<String, NavigationPageRegistration> registrationsByUrl(
            @NonNull List<NavigationPageRegistration> registrations) {
        final Map<String, NavigationPageRegistration> registrationsByUrl = new HashMap<>();
        for (NavigationPageRegistration registration : registrations) {
            final String relativeUrl = registration.model().relativeUrl();
            final NavigationPageRegistration existingRegistration = registrationsByUrl.put(relativeUrl, registration);
            if (existingRegistration != null) {
                throw new IllegalStateException("Duplicate navigation page URL: " + relativeUrl);
            }
        }
        return Map.copyOf(registrationsByUrl);
    }

    private Map<String, List<NavigationPageRegistration>> childrenByParentUrl(
            @NonNull List<NavigationPageRegistration> registrations,
            @NonNull Map<String, NavigationPageRegistration> registrationsByUrl) {
        final Map<String, List<NavigationPageRegistration>> childrenByParentUrl = new HashMap<>();
        for (NavigationPageRegistration registration : registrations) {
            registration.parentRelativeUrl().ifPresent(parentRelativeUrl -> {
                if (!registrationsByUrl.containsKey(parentRelativeUrl)) {
                    throw new IllegalStateException(
                            "Navigation page parent does not exist: " + parentRelativeUrl);
                }
                if (parentRelativeUrl.equals(registration.model().relativeUrl())) {
                    throw new IllegalStateException("Navigation page cannot be its own parent: " + parentRelativeUrl);
                }
                childrenByParentUrl.merge(
                        parentRelativeUrl,
                        List.of(registration),
                        this::append);
            });
        }
        return Map.copyOf(childrenByParentUrl);
    }

    private List<NavigationPageRegistration> append(@NonNull List<NavigationPageRegistration> existing,
                                                    @NonNull List<NavigationPageRegistration> additional) {
        return Stream.concat(existing.stream(), additional.stream()).toList();
    }

    private List<SubPage> subPages(@NonNull String parentRelativeUrl,
                                   @NonNull Map<String, List<NavigationPageRegistration>> childrenByParentUrl) {
        return childrenByParentUrl.getOrDefault(parentRelativeUrl, List.of())
                .stream()
                .sorted(REGISTRATION_ORDER)
                .map(registration -> new SubPage(
                        registration.model(),
                        subPages(registration.model().relativeUrl(), childrenByParentUrl)))
                .toList();
    }

    private void validateReachablePages(@NonNull List<MainPage> mainPages,
                                        @NonNull List<NavigationPageRegistration> registrations) {
        final long reachablePageCount = mainPages.stream()
                .flatMap(this::pageAndSubPages)
                .count();
        if (reachablePageCount != registrations.size()) {
            throw new IllegalStateException("All navigation pages must be reachable from a main page");
        }
    }

    private Stream<NavigablePage> pageAndSubPages(@NonNull NavigablePage page) {
        return Stream.concat(
                Stream.of(page),
                page.subPages().stream().flatMap(this::pageAndSubPages));
    }

    private boolean matches(@NonNull String currentPath, @NonNull String relativeUrl) {
        if (relativeUrl.isBlank()) {
            return false;
        }
        if ("/".equals(relativeUrl)) {
            return "/".equals(currentPath);
        }
        return currentPath.equals(relativeUrl) || currentPath.startsWith(relativeUrl + "/");
    }
}
