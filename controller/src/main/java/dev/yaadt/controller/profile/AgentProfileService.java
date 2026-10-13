package dev.yaadt.controller.profile;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import dev.yaadt.controller.user.CurrentUserService;

@Service
@Transactional
public class AgentProfileService {
    private final AgentProfileRepository profiles;
    private final AgentProfileVersionRepository versions;
    private final CurrentUserService users;
    private final Clock clock;

    public AgentProfileService(AgentProfileRepository profiles, AgentProfileVersionRepository versions,
            CurrentUserService users, Clock clock) {
        this.profiles = profiles; this.versions = versions; this.users = users; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AgentProfileResponse> list() {
        return profiles.findAll().stream().map(profile -> AgentProfileResponse.from(profile,
                versions.findTopByProfileIdOrderByVersionDesc(profile.getId()).orElseThrow())).toList();
    }

    public AgentProfileResponse create(AgentProfileRequest request, String subject, String displayName) {
        AgentProfile profile = profiles.save(new AgentProfile(request.name(), request.role()));
        AgentProfileVersion version = versions.save(new AgentProfileVersion(profile.getId(), 1, request,
                users.resolve(subject, displayName).getId(), Instant.now(clock)));
        return AgentProfileResponse.from(profile, version);
    }

    public AgentProfileResponse addVersion(UUID profileId, AgentProfileRequest request, String subject, String displayName) {
        AgentProfile profile = profiles.findById(profileId).orElseThrow(() -> notFound("Agent profile not found"));
        int next = versions.findTopByProfileIdOrderByVersionDesc(profileId).map(v -> v.getVersion() + 1).orElse(1);
        AgentProfileVersion version = versions.save(new AgentProfileVersion(profile.getId(), next, request,
                users.resolve(subject, displayName).getId(), Instant.now(clock)));
        return AgentProfileResponse.from(profile, version);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
