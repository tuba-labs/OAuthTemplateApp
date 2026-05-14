package org.tubalabs.app.users.db.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tubalabs.app.security.identity.ExternalIdentity;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    private final UserProfileMapper userProfileMapper;

    public UserProfileDbo createInitialProfile(UUID userId, ExternalIdentity externalIdentity) {
        final UserProfileDbo profile = userProfileMapper.toProfile(userId, externalIdentity);
        return userProfileRepository.insert(profile);
    }
}