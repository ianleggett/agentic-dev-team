package dev.yaadt.controller.beads;

public final class BeadsProjectLabels {
    private BeadsProjectLabels() { }

    public static String forProject(String beadsProject) {
        if (beadsProject == null || beadsProject.isBlank()) {
            throw new IllegalArgumentException("Beads project must not be blank");
        }
        return "project:" + beadsProject.trim();
    }
}
