package dev.yaadt.controller.directive;

import java.time.Instant;
import java.util.UUID;

public record DirectiveResponse(UUID id, UUID projectId, int version, String content, boolean active, Instant createdAt) {
    public static DirectiveResponse from(DirectiveRevision d) {
        return new DirectiveResponse(d.getId(), d.getProjectId(), d.getVersion(), d.getContent(), d.isActive(), d.getCreatedAt());
    }
}
