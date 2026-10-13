package dev.yaadt.controller.user;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository users;
    private final Clock clock;

    public CurrentUserService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public User resolve(String subject, String displayName) {
        String safeSubject = subject == null || subject.isBlank() ? "local-user" : subject;
        String safeName = displayName == null || displayName.isBlank() ? safeSubject : displayName;
        return users.findBySubject(safeSubject)
                .orElseGet(() -> users.save(new User(safeSubject, safeName, Instant.now(clock))));
    }
}
