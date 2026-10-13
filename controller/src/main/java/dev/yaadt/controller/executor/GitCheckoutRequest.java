package dev.yaadt.controller.executor;

import java.util.UUID;

public record GitCheckoutRequest(
        UUID repositoryId,
        String remoteUrl,
        String branch,
        String relativeWorkspacePath) {
}
