package dev.yaadt.controller.profile;

import java.time.Instant;
import java.util.UUID;

public record AgentProfileResponse(
        UUID id, String name, String role, boolean active, UUID versionId, int version,
        String modelIdentifier, String providerIdentifier, String promptReference, String baseUrl,
        boolean apiKeyConfigured, String teamPrompt,
        String toolPolicy, String permissionPolicy, String environmentPolicy,
        int maxSteps, int timeoutSeconds, Instant createdAt) {

    public static AgentProfileResponse from(AgentProfile profile, AgentProfileVersion version) {
        return new AgentProfileResponse(profile.getId(), profile.getName(), profile.getRole(), profile.isActive(),
                version.getId(), version.getVersion(), version.getModelIdentifier(), version.getProviderIdentifier(),
                version.getPromptReference(), version.getBaseUrl(), version.isApiKeyConfigured(), version.getTeamPrompt(),
                version.getToolPolicy(), version.getPermissionPolicy(),
                version.getEnvironmentPolicy(), version.getMaxSteps(), version.getTimeoutSeconds(), version.getCreatedAt());
    }
}
