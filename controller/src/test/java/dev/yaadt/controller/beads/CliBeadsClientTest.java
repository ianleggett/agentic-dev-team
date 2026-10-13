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
    }

    private static final class TestClient extends CliBeadsClient {

        private final String json;

        private TestClient(String json) {
            super(new ObjectMapper(), new BeadsProperties());
            this.json = json;
        }

        @Override
        public List<BeadsIssue> listRelevantIssues(String projectLabel) {
            try {
                var root = new ObjectMapper().readTree(json);
                return java.util.stream.StreamSupport.stream(root.spliterator(), false)
                        .map(node -> new BeadsIssue(
                                node.path("id").asText(),
                                node.path("title").asText(),
                                node.path("description").asText(),
                                node.path("status").asText(),
                                java.util.stream.StreamSupport.stream(node.path("labels").spliterator(), false)
                                        .map(com.fasterxml.jackson.databind.JsonNode::asText)
                                        .toList()))
                        .filter(issue -> issue.labels().contains(projectLabel))
                        .toList();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }
}
