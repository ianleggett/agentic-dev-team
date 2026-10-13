package dev.yaadt.controller.beads;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class CliBeadsClient implements BeadsClient {

    private final ObjectMapper objectMapper;
    private final BeadsProperties properties;

    public CliBeadsClient(ObjectMapper objectMapper, BeadsProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public List<BeadsIssue> listRelevantIssues(String projectLabel) {
        JsonNode root = execute(List.of("list", "--json"));
        List<BeadsIssue> issues = new ArrayList<>();
        for (JsonNode node : array(root)) {
            BeadsIssue issue = parseIssue(node);
            if (issue.labels().contains(projectLabel)) {
                issues.add(issue);
            }
        }
        return issues;
    }

    @Override
    public BeadsIssue getIssue(String issueId) {
        return parseIssue(first(execute(List.of("show", issueId, "--json"))));
    }

    @Override
    public BeadsIssue createIssue(CreateBeadsIssue request) {
        List<String> command = new ArrayList<>(List.of(
                "create", request.title(), "--type", "task", "--description", request.description(),
                "--labels", String.join(",", request.labels()), "--json"));
        return parseIssue(first(execute(command)));
    }

    @Override
    public void claimIssue(String issueId) {
        execute(List.of("update", issueId, "--claim", "--json"));
    }

    @Override
    public void addComment(String issueId, String comment) {
        execute(List.of("comment", issueId, comment));
    }

    private JsonNode execute(List<String> arguments) {
        List<String> command = new ArrayList<>();
        command.add(properties.getCommand());
        command.addAll(arguments);

        Process process;
        try {
            ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(Path.of(properties.getDirectory()).toFile());
            Map<String, String> environment = builder.environment();
            environment.put("BEADS_DIR", properties.getBeadsDir());
            environment.put("BEADS_ACTOR", properties.getActor());
            process = builder.start();
            CompletableFuture<String> errorOutput = CompletableFuture.supplyAsync(() -> read(process.getErrorStream()));
            String output = read(process.getInputStream());
            if (!process.waitFor(properties.getTimeout().toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new BeadsCommandException("Beads command timed out: " + String.join(" ", command));
            }
            String error = errorOutput.join();
            if (process.exitValue() != 0) {
                throw new BeadsCommandException("Beads command failed (" + process.exitValue() + "): " + error);
            }
            if (arguments.get(arguments.size() - 1).equals("--json")) {
                return parseJson(output);
            }
            return objectMapper.createObjectNode();
        } catch (IOException e) {
            throw new BeadsCommandException("Unable to execute Beads command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BeadsCommandException("Beads command interrupted");
        }
    }

    private String read(java.io.InputStream stream) {
        try {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BeadsCommandException("Unable to read Beads command output: " + e.getMessage());
        }
    }

    private JsonNode parseJson(String output) {
        try {
            return objectMapper.readTree(output);
        } catch (JsonProcessingException e) {
            throw new BeadsCommandException("Beads returned invalid JSON: " + output);
        }
    }

    private BeadsIssue parseIssue(JsonNode node) {
        List<String> labels = new ArrayList<>();
        node.path("labels").forEach(label -> labels.add(label.asText()));
        return new BeadsIssue(
                node.path("id").asText(),
                node.path("title").asText(),
                node.path("description").asText(null),
                node.path("status").asText(),
                List.copyOf(labels));
    }

    private JsonNode first(JsonNode root) {
        return root.isArray() ? root.path(0) : root;
    }

    private List<JsonNode> array(JsonNode root) {
        List<JsonNode> nodes = new ArrayList<>();
        if (root.isArray()) {
            root.forEach(nodes::add);
        }
        return nodes;
    }
}
