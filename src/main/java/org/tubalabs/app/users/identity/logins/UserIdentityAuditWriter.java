package org.tubalabs.app.users.identity.logins;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tubalabs.app.events.EventLog;
import org.tubalabs.app.events.EventLogService;
import org.tubalabs.app.users.identity.logins.db.UserLoginDbo;
import org.tubalabs.app.users.identity.logins.db.UserLoginRepository;

@Component
@RequiredArgsConstructor
public class UserIdentityAuditWriter {

    private final @NonNull UserLoginRepository userLoginRepository;
    private final @NonNull EventLogService eventLogService;

    @Transactional(propagation = Propagation.NESTED)
    public void insertLogin(@NonNull UserLoginDbo login) {
        userLoginRepository.insert(login);
    }

    @Transactional(propagation = Propagation.NESTED)
    public void insertEvent(@NonNull EventLog eventLog) {
        eventLogService.record(eventLog);
    }
}
