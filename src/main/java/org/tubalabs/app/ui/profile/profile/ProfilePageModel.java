package org.tubalabs.app.ui.profile.profile;

import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.tubalabs.app.users.current.CurrentUser;
import org.tubalabs.app.users.profile.db.UserProfileDbo;

@Component
public class ProfilePageModel {

    public static final String LOCAL_PROFILE_ATTRIBUTE = "localProfile";
    public static final String LOCAL_LOGIN_NAME_ATTRIBUTE = "localLoginName";
    public static final String PROFILE_PICTURE_ATTRIBUTE = "profilePictureUrl";

    void addProfileAttributes(@NonNull Model model,
                              @NonNull CurrentUser currentUser,
                              @NonNull UserProfileDbo profile) {
        model.addAttribute(LOCAL_PROFILE_ATTRIBUTE, currentUser.localProfile());
        if (currentUser.localProfile()) {
            model.addAttribute(LOCAL_LOGIN_NAME_ATTRIBUTE, currentUser.localLoginName());
        }
        model.addAttribute(PROFILE_PICTURE_ATTRIBUTE, profile.pictureUrl());
    }
}
