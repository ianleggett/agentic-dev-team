package dev.yaadt.controller.beads;

public record BeadsCapabilities(
        boolean available,
        String version,
        boolean jsonOutput,
        boolean claim,
        String error) {

    public static BeadsCapabilities unavailable(String error) {
        return new BeadsCapabilities(false, "", false, false, error);
    }
}
