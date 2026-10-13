package dev.yaadt.controller.repository;

import dev.yaadt.controller.executor.GitCheckoutResult;

public record RepositoryCheckoutResponse(String workspacePath, String branch, String commitSha, String action) {
    public static RepositoryCheckoutResponse from(GitCheckoutResult result) {
        return new RepositoryCheckoutResponse(result.workspacePath().toString(), result.branch(),
                result.commitSha(), result.action().name());
    }
}
