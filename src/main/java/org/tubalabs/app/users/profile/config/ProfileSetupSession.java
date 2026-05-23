package org.tubalabs.app.users.profile.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.NonNull;
import org.springframework.stereotype.Service;

@Service
public class ProfileSetupSession {

    private static final String PROFILE_SETUP_REQUIRED_ATTRIBUTE = "profileSetupRequired";
    private static final String PROFILE_SETUP_CHECKED_ATTRIBUTE = "profileSetupChecked";

    public void requireProfileSetup(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession();
        session.setAttribute(PROFILE_SETUP_REQUIRED_ATTRIBUTE, Boolean.TRUE);
        session.setAttribute(PROFILE_SETUP_CHECKED_ATTRIBUTE, Boolean.TRUE);
    }

    public boolean isProfileSetupRequired(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(PROFILE_SETUP_REQUIRED_ATTRIBUTE));
    }

    public boolean hasCheckedProfileSetup(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(PROFILE_SETUP_CHECKED_ATTRIBUTE));
    }

    public void completeProfileSetup(@NonNull HttpServletRequest request) {
        final HttpSession session = request.getSession();
        session.removeAttribute(PROFILE_SETUP_REQUIRED_ATTRIBUTE);
        session.setAttribute(PROFILE_SETUP_CHECKED_ATTRIBUTE, Boolean.TRUE);
    }
}
