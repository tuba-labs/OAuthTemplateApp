package org.tubalabs.app.navigation.ui;

import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.tubalabs.app.localization.LocalizationService;
import org.tubalabs.app.navigation.NavigationCatalog;
import org.tubalabs.app.navigation.NavigationPageRegistration;
import org.tubalabs.app.navigation.PageText;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuDto;
import org.tubalabs.app.navigation.ui.dtos.NavigationMenuItemDto;
import org.tubalabs.app.ui.profile.changepassword.ProfileChangePasswordPage;
import org.tubalabs.app.ui.profile.logintypes.menusystem.ProfileLoginTypesPage;
import org.tubalabs.app.ui.profile.logintypes.local.ProfileLocalLoginTypePage;
import org.tubalabs.app.ui.profile.profile.menusystem.ProfilePage;
import org.tubalabs.app.ui.startpage.menusystem.HomePage;
import org.tubalabs.app.users.current.CurrentUser;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationPageModelTest {

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DISPLAY_NAME = "Person Name";
    private static final String EMAIL = "person@example.com";
    private static final String HOME_LABEL = "Home";
    private static final String PROFILE_LABEL = "Profile";
    private static final String LOGIN_TYPES_LABEL = "Login types";
    private static final String CHANGE_PASSWORD_LABEL = "Change password";
    private static final String LINK_EMAIL_PASSWORD_LABEL = "Link email and password";
    private static final String NORWEGIAN_HOME_LABEL = "Hjem";
    private static final String NORWEGIAN_PROFILE_LABEL = "Profil";
    private static final String NORWEGIAN_LOGIN_TYPES_LABEL = "Innloggingstyper";
    private static final String NORWEGIAN_CHANGE_PASSWORD_LABEL = "Endre passord";
    private static final String NORWEGIAN_LINK_EMAIL_PASSWORD_LABEL = "Koble til e-post og passord";
    private static final String HOME_TOOLTIP = "Home";
    private static final String PROFILE_TOOLTIP = "Manage your profile";
    private static final String LOGIN_TYPES_TOOLTIP = "Manage login types";
    private static final String CHANGE_PASSWORD_TOOLTIP = "Change your email login password";
    private static final String LINK_EMAIL_PASSWORD_TOOLTIP = "Add email and password login";
    private static final Locale TEST_LOCALE = Locale.ENGLISH;
    private static final Locale NORWEGIAN_LOCALE = Locale.forLanguageTag("nb");
    private static final List<NavigationPageRegistration> PAGE_REGISTRATIONS = List.of(
            new HomePage(),
            new ProfilePage(),
            new ProfileLoginTypesPage(),
            new ProfileChangePasswordPage(),
            new ProfileLocalLoginTypePage());

    private final NavigationPageModel navigationPageModel =
            new NavigationPageModel(new NavigationCatalog(PAGE_REGISTRATIONS), localizationService());

    @BeforeEach
    void setLocale() {
        LocaleContextHolder.setLocale(TEST_LOCALE);
    }

    @AfterEach
    void resetLocale() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void buildsNavigationTreeForLocalCurrentUser() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, EMAIL, false);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/password");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, PROFILE_LABEL);
        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::relativeUrl)
                .containsExactly("/", "/profile");
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.primaryItems().get(1).active()).isFalse();
        assertThat(navigationMenu.primaryItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(LOGIN_TYPES_LABEL, CHANGE_PASSWORD_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children().get(1).tooltip())
                .isEqualTo(CHANGE_PASSWORD_TOOLTIP);
        assertThat(navigationMenu.primaryItems().get(1).children().get(1).active()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(PROFILE_LABEL, CHANGE_PASSWORD_LABEL);
    }

    @Test
    void hidesLocalLoginLinkFromNavigationForExternalCurrentUser() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, null, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/login-types");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, PROFILE_LABEL);
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.primaryItems().get(1).active()).isFalse();
        assertThat(navigationMenu.primaryItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(LOGIN_TYPES_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children().getFirst().active()).isTrue();
        assertThat(navigationMenu.primaryItems().get(1).children().getFirst().children()).isEmpty();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(PROFILE_LABEL, LOGIN_TYPES_LABEL);
    }

    @Test
    void keepsLoginTypesVisibleDuringProfileSetup() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, true, EMAIL, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, PROFILE_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(LOGIN_TYPES_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children().getFirst().children()).isEmpty();
        assertThat(navigationMenu.primaryItems().get(1).active()).isTrue();
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(PROFILE_LABEL);
    }

    @Test
    void keepsHomeFirstWhenHomeIsActive() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, EMAIL, true);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL, PROFILE_LABEL);
        assertThat(navigationMenu.primaryItems().getFirst().label()).isEqualTo(HOME_LABEL);
        assertThat(navigationMenu.primaryItems().getFirst().hasChildren()).isFalse();
        assertThat(navigationMenu.primaryItems().getFirst().active()).isTrue();
        assertThat(navigationMenu.hasMenuItems()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(HOME_LABEL);
    }

    @Test
    void buildsBreadcrumbsForNestedActivePage() {
        final CurrentUser currentUser = new CurrentUser(USER_ID, DISPLAY_NAME, null, false, null, true);

        final NavigationMenuDto navigationMenu =
                navigationPageModel.navigationMenu(currentUser, "/profile/login-types/local/link");

        assertThat(navigationMenu.hasBreadcrumbs()).isTrue();
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(PROFILE_LABEL, LOGIN_TYPES_LABEL, LINK_EMAIL_PASSWORD_LABEL);
    }

    @Test
    void localizesPageLabelsFromCurrentLocale() {
        LocaleContextHolder.setLocale(NORWEGIAN_LOCALE);
        final CurrentUser currentUser = new CurrentUser(USER_ID, null, null, false, EMAIL, false);

        final NavigationMenuDto navigationMenu = navigationPageModel.navigationMenu(currentUser, "/profile/password");

        assertThat(navigationMenu.primaryItems())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(NORWEGIAN_HOME_LABEL, NORWEGIAN_PROFILE_LABEL);
        assertThat(navigationMenu.primaryItems().get(1).children())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(NORWEGIAN_LOGIN_TYPES_LABEL, NORWEGIAN_CHANGE_PASSWORD_LABEL);
        assertThat(navigationMenu.breadcrumbs())
                .extracting(NavigationMenuItemDto::label)
                .containsExactly(NORWEGIAN_PROFILE_LABEL, NORWEGIAN_CHANGE_PASSWORD_LABEL);
    }

    private static LocalizationService localizationService() {
        final StaticMessageSource messageSource = new StaticMessageSource();
        addPageMessages(messageSource, new HomePage().model().text(), HOME_LABEL, HOME_TOOLTIP);
        addPageMessages(messageSource, new ProfilePage().model().text(), PROFILE_LABEL, PROFILE_TOOLTIP);
        addPageMessages(messageSource, new ProfileLoginTypesPage().model().text(), LOGIN_TYPES_LABEL, LOGIN_TYPES_TOOLTIP);
        addPageMessages(
                messageSource,
                new ProfileChangePasswordPage().model().text(),
                CHANGE_PASSWORD_LABEL,
                CHANGE_PASSWORD_TOOLTIP);
        addPageMessages(
                messageSource,
                new ProfileLocalLoginTypePage().model().text(),
                LINK_EMAIL_PASSWORD_LABEL,
                LINK_EMAIL_PASSWORD_TOOLTIP);
        addPageMessages(messageSource, new HomePage().model().text(), NORWEGIAN_HOME_LABEL, HOME_TOOLTIP, NORWEGIAN_LOCALE);
        addPageMessages(
                messageSource,
                new ProfilePage().model().text(),
                NORWEGIAN_PROFILE_LABEL,
                PROFILE_TOOLTIP,
                NORWEGIAN_LOCALE);
        addPageMessages(
                messageSource,
                new ProfileLoginTypesPage().model().text(),
                NORWEGIAN_LOGIN_TYPES_LABEL,
                LOGIN_TYPES_TOOLTIP,
                NORWEGIAN_LOCALE);
        addPageMessages(
                messageSource,
                new ProfileChangePasswordPage().model().text(),
                NORWEGIAN_CHANGE_PASSWORD_LABEL,
                CHANGE_PASSWORD_TOOLTIP,
                NORWEGIAN_LOCALE);
        addPageMessages(
                messageSource,
                new ProfileLocalLoginTypePage().model().text(),
                NORWEGIAN_LINK_EMAIL_PASSWORD_LABEL,
                LINK_EMAIL_PASSWORD_TOOLTIP,
                NORWEGIAN_LOCALE);
        return new LocalizationService(messageSource);
    }

    private static void addPageMessages(@NonNull StaticMessageSource messageSource,
                                        @NonNull PageText text,
                                        @NonNull String label,
                                        @NonNull String tooltip) {
        addPageMessages(messageSource, text, label, tooltip, TEST_LOCALE);
    }

    private static void addPageMessages(@NonNull StaticMessageSource messageSource,
                                        @NonNull PageText text,
                                        @NonNull String label,
                                        @NonNull String tooltip,
                                        @NonNull Locale locale) {
        messageSource.addMessage(text.labelLocalizationKey(), locale, label);
        messageSource.addMessage(text.tooltipLocalizationKey(), locale, tooltip);
    }
}
