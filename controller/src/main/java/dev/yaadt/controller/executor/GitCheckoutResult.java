package dev.yaadt.controller.executor;

import java.nio.file.Path;

public record GitCheckoutResult(
        Path workspacePath,
        String branch,
        String commitSha,
        Action action) {
    public enum Action { CLONED, UPDATED }
}
