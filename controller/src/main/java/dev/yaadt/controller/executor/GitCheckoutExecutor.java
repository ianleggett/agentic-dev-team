package dev.yaadt.controller.executor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitCheckoutExecutor {
    private static final int MAX_OUTPUT = 16_000;
    private final Path workspaceRoot;
    private final Duration timeout;

    public GitCheckoutExecutor(
            @Value("${yaadt.executor.workspace-root:/workspace}") String workspaceRoot,
            @Value("${yaadt.executor.git-timeout:10m}") Duration timeout) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
        this.timeout = timeout;
    }

    public GitCheckoutResult checkout(GitCheckoutRequest request) {
        validate(request);
        Path destination = resolveDestination(request.relativeWorkspacePath());
        boolean clone = !Files.exists(destination);
        try {
            if (clone) {
                try {
                    Files.createDirectories(destination.getParent());
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to create checkout parent", exception);
                }
                run(List.of("git", "clone", "--branch", request.branch(), "--single-branch",
                        request.remoteUrl(), destination.toString()), workspaceRoot);
            } else {
                validateExistingCheckout(destination, request.remoteUrl());
                run(List.of("git", "-C", destination.toString(), "fetch", "--prune", "origin", request.branch()), workspaceRoot);
                run(List.of("git", "-C", destination.toString(), "checkout", "--force", request.branch()), workspaceRoot);
                run(List.of("git", "-C", destination.toString(), "reset", "--hard", "origin/" + request.branch()), workspaceRoot);
            }
            String sha = run(List.of("git", "-C", destination.toString(), "rev-parse", "HEAD"), workspaceRoot).trim();
            return new GitCheckoutResult(destination, request.branch(), sha, clone
                    ? GitCheckoutResult.Action.CLONED : GitCheckoutResult.Action.UPDATED);
        } catch (RuntimeException failure) {
            if (clone) {
                deleteQuietly(destination);
            }
            throw failure;
        }
    }

    private Path resolveDestination(String relativePath) {
        Path relative = Path.of(relativePath);
        if (relative.isAbsolute() || relative.getNameCount() == 0
                || relative.iterator().next().toString().equals(".")) {
            throw new IllegalArgumentException("relativeWorkspacePath must be a non-empty relative path");
        }
        for (Path part : relative) {
            if (part.toString().equals("..")) {
                throw new IllegalArgumentException("relativeWorkspacePath cannot contain '..'");
            }
        }
        Path destination = workspaceRoot.resolve(relative).normalize();
        if (!destination.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("checkout path escapes the workspace root");
        }
        return destination;
    }

    private void validate(GitCheckoutRequest request) {
        Objects.requireNonNull(request, "request");
        requireSafeText(request.remoteUrl(), "remoteUrl");
        requireSafeText(request.branch(), "branch");
        requireSafeText(request.relativeWorkspacePath(), "relativeWorkspacePath");
        if (!(request.remoteUrl().startsWith("https://") || request.remoteUrl().startsWith("http://")
                || request.remoteUrl().startsWith("ssh://") || request.remoteUrl().startsWith("git@"))) {
            throw new IllegalArgumentException("remoteUrl must use HTTPS, SSH, or SCP-style SSH");
        }
        if (!request.branch().matches("[A-Za-z0-9][A-Za-z0-9._/-]*")
                || request.branch().contains("..") || request.branch().endsWith("/")
                || request.branch().endsWith(".")) {
            throw new IllegalArgumentException("invalid Git branch name");
        }
    }

    private void validateExistingCheckout(Path destination, String remoteUrl) {
        if (!Files.isDirectory(destination) || Files.isSymbolicLink(destination)
                || !Files.isDirectory(destination.resolve(".git"))) {
            throw new IllegalStateException("checkout destination is not a Git worktree: " + destination);
        }
        String origin = run(List.of("git", "-C", destination.toString(), "remote", "get-url", "origin"), workspaceRoot).trim();
        if (!origin.equals(remoteUrl)) {
            throw new IllegalStateException("Git origin does not match the configured remote");
        }
    }

    private String run(List<String> command, Path directory) {
        try {
            Process process = new ProcessBuilder(command).directory(directory.toFile()).redirectErrorStream(true).start();
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Git command timed out");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IllegalStateException("Git command failed: " + truncate(output));
            }
            return truncate(output);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start Git", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Git command interrupted", exception);
        }
    }

    private String truncate(String value) {
        return value.length() <= MAX_OUTPUT ? value : value.substring(0, MAX_OUTPUT);
    }

    private void requireSafeText(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(name + " is required and cannot contain newlines");
        }
    }

    private void deleteQuietly(Path path) {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            paths.sorted((left, right) -> right.getNameCount() - left.getNameCount()).forEach(candidate -> {
                try { Files.deleteIfExists(candidate); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
