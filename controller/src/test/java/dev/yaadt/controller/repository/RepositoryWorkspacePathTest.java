package dev.yaadt.controller.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RepositoryWorkspacePathTest {
    @Test
    void legacyTraversalFallsBackToSanitizedRepositoryName() {
        Repository repository = new Repository(UUID.randomUUID(), "safe-repo", "remote", "main", "product",
                "../outside", "build", "test", null, Instant.EPOCH);

        assertEquals("safe-repo", repository.effectiveWorkspacePath());
    }
}
