package org.tubalabs.app.users.profile;

import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSetupRequirementService {

    private final @NonNull ProfileSetupSession profileSetupSession;
    private final @NonNull ProfileCompletenessService profileCompletenessService;

    public boolean requireSetupIfProfileIncomplete(@NonNull HttpServletRequest request, @NonNull UUID userId) {
        if (profileCompletenessService.isProfileComplete(userId)) {
            profileSetupSession.completeProfileSetup(request);
            return false;
        }
        profileSetupSession.requireProfileSetup(request);
        return true;
    }

    public boolean isSetupRequiredForSession(@NonNull HttpServletRequest request, @NonNull UUID userId) {
        if (!profileSetupSession.hasCheckedProfileSetup(request)) {
            requireSetupIfProfileIncomplete(request, userId);
        }
        return profileSetupSession.isProfileSetupRequired(request);
    }
}
