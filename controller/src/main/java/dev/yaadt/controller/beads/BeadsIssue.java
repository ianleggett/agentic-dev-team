package dev.yaadt.controller.beads;

import java.util.List;

public record BeadsIssue(
        String id,
        String title,
        String description,
        String status,
        List<String> labels) {
}
