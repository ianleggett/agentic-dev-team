package dev.yaadt.controller.beads;

import java.util.List;

public interface BeadsClient {

    List<BeadsIssue> listRelevantIssues(String projectLabel);

    BeadsIssue getIssue(String issueId);

    BeadsIssue createIssue(CreateBeadsIssue request);

    void claimIssue(String issueId);

    void addComment(String issueId, String comment);

    record CreateBeadsIssue(String title, String description, List<String> labels) {
    }
}
