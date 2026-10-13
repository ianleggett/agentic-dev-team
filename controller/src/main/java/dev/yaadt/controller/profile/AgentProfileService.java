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

    @Transactional(readOnly = true)
    public AgentProfileResponse get(UUID id) {
        AgentProfile profile = profiles.findById(id).orElseThrow(() -> notFound("Agent profile not found"));
        return AgentProfileResponse.from(profile, versions.findTopByProfileIdOrderByVersionDesc(id).orElseThrow());
    }

    public AgentProfileResponse addVersion(UUID profileId, AgentProfileRequest request, String subject, String displayName) {
        AgentProfile profile = profiles.findById(profileId).orElseThrow(() -> notFound("Agent profile not found"));
        AgentProfileVersion previous = versions.findTopByProfileIdOrderByVersionDesc(profileId).orElse(null);
        int next = previous == null ? 1 : previous.getVersion() + 1;
        AgentProfileRequest effectiveRequest = request;
        if (previous != null && (request.apiKey() == null || request.apiKey().isBlank())) {
            effectiveRequest = new AgentProfileRequest(request.name(), request.role(), request.modelIdentifier(),
                    request.providerIdentifier(), request.baseUrl(), previous.getApiKey(), request.promptReference(),
                    request.teamPrompt(), request.toolPolicy(), request.permissionPolicy(), request.environmentPolicy(),
                    request.maxSteps(), request.timeoutSeconds());
        }
        AgentProfileVersion version = versions.save(new AgentProfileVersion(profile.getId(), next, effectiveRequest,
                users.resolve(subject, displayName).getId(), Instant.now(clock)));
        return AgentProfileResponse.from(profile, version);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
