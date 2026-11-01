package dev.yaadt.controller.beads;

import java.util.List;

public interface BeadsClient {

    List<BeadsIssue> listRelevantIssues(String projectLabel);

    BeadsIssue getIssue(String issueId);

    BeadsIssue createIssue(CreateBeadsIssue request);

    void removeLabel(String issueId, String label);

    void claimIssue(String issueId);

    void releaseClaim(String issueId);

    void closeIssue(String issueId, String reason);

    default void closeIssueIfOpen(String issueId, String reason) {
        BeadsIssue issue = getIssue(issueId);
        if (BeadsStatus.isTerminal(issue.status())) return;
        closeIssue(issueId, reason);
    }

    void addComment(String issueId, String comment);

    default void addCommentIfAbsent(String issueId, String comment) {
        addComment(issueId, comment);
    }

    BeadsCapabilities capabilities();

    record CreateBeadsIssue(String title, String description, List<String> labels) {
    }
}
