package dev.yaadt.controller.beads;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class CliBeadsClientTest {

    @Test
    void parsesIssueListsAndFiltersByProjectLabel() {
        String json = """
                [
                  {"id":"bd-1","title":"First","description":"one","status":"open","labels":["project:yaadt","worker:fullstack"]},
                  {"id":"bd-2","title":"Other","description":"two","status":"open","labels":["project:other"]}
                ]
                """;

        TestClient client = new TestClient(json);

        assertThat(client.listRelevantIssues("project:yaadt"))
                .extracting(BeadsIssue::id)
                .containsExactly("bd-1");
        assertThat(client.lastCommand).containsExactly("list", "--all", "--json");
    }

    @Test
    void closesIssueWithCompletionReason() {
        TestClient client = new TestClient("[]");

        client.closeIssue("bd-1", "Sprint completed successfully: sprint-1");

        assertThat(client.lastCommand)
                .containsExactly("close", "bd-1", "--reason", "Sprint completed successfully: sprint-1");
    }

    @Test
    void releasesClaimWithoutChangingOtherIssueState() {
        TestClient client = new TestClient("[]");
        client.assignee = "yaadt-controller";

        client.releaseClaim("bd-1");

        assertThat(client.lastCommand)
                .containsExactly("update", "bd-1", "--assignee", "", "--json");
    }

    @Test
    void claimIsIdempotentForCurrentActor() {
        TestClient client = new TestClient("[]");
        client.assignee = "yaadt-controller";

        client.claimIssue("bd-1");

        assertThat(client.lastCommand).containsExactly("show", "bd-1", "--json");
    }

    @Test
    void commentsAreNotDuplicated() {
        TestClient client = new TestClient("[]");
        client.comments = new ObjectMapper().createArrayNode()
                .add(new ObjectMapper().createObjectNode().put("text", "already recorded"));

        client.addCommentIfAbsent("bd-1", "already recorded");

        assertThat(client.lastCommand).containsExactly("comments", "bd-1", "--json");
    }

    @Test
    void detectsAndCachesCliCapabilities() {
        TestClient client = new TestClient("[]");
        client.commandOutput.put("--version", "beads 0.9.0");
        client.commandOutput.put("help list", "list --json");
        client.commandOutput.put("help update", "update --claim");

        BeadsCapabilities first = client.capabilities();
        BeadsCapabilities second = client.capabilities();

        assertThat(first).isEqualTo(new BeadsCapabilities(true, "beads 0.9.0", true, true, ""));
        assertThat(second).isSameAs(first);
        assertThat(client.textCommandCount).isEqualTo(3);
    }

    @Test
    void reportsCliOutageWithoutThrowingFromCapabilityProbe() {
        TestClient client = new TestClient("[]");
        client.failTextCommands = true;

        assertThat(client.capabilities().available()).isFalse();
        assertThat(client.capabilities().error()).contains("unavailable");
    }

    @Test
    void retriesCapabilityProbeAfterAnOutage() {
        TestClient client = new TestClient("[]");
        client.failTextCommands = true;
        assertThat(client.capabilities().available()).isFalse();

        client.failTextCommands = false;
        client.commandOutput.put("--version", "beads 0.9.0");
        client.commandOutput.put("help list", "list --json");
        client.commandOutput.put("help update", "update --claim");

        assertThat(client.capabilities().available()).isTrue();
        assertThat(client.textCommandCount).isEqualTo(4);
    }

    private static final class TestClient extends CliBeadsClient {

        private final String json;
        private List<String> lastCommand;
        private final java.util.Map<String, String> commandOutput = new java.util.HashMap<>();
        private int textCommandCount;
        private boolean failTextCommands;
        private String assignee;
        private com.fasterxml.jackson.databind.JsonNode comments = new ObjectMapper().createArrayNode();

        private TestClient(String json) {
            super(new ObjectMapper(), new BeadsProperties());
            this.json = json;
        }

        @Override
        protected com.fasterxml.jackson.databind.JsonNode execute(List<String> arguments) {
            lastCommand = List.copyOf(arguments);
            if (arguments.size() >= 2 && arguments.get(0).equals("show")) {
                return new ObjectMapper().createObjectNode().put("assignee", assignee == null ? "" : assignee);
            }
            if (arguments.size() >= 2 && arguments.get(0).equals("comments")) return comments;
            if (arguments.contains("list")) {
                try {
                    return new ObjectMapper().readTree(json);
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }
            return new ObjectMapper().createObjectNode();
        }

        @Override
        protected String executeText(List<String> arguments) {
            textCommandCount++;
            if (failTextCommands) throw new BeadsCommandException("Beads command unavailable");
            return commandOutput.getOrDefault(String.join(" ", arguments), "");
        }
    }
}
