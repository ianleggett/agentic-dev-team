package dev.yaadt.controller.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "yaadt.agent", name = "executor", havingValue = "opencode")
public class OpenCodeAgentExecutor implements AgentExecutor {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final String runnerUrl;
    private final String token;

    public OpenCodeAgentExecutor(ObjectMapper mapper,
            @Value("${yaadt.agent.runner-url:http://agent-runner:3010}") String runnerUrl,
            @Value("${yaadt.agent.runner-token:}") String token) {
        this.mapper = mapper; this.runnerUrl = runnerUrl; this.token = token;
    }

    @Override
    public AgentRunResult execute(AgentRunRequest request) {
        try {
            AgentBackendConfig backend = request.backend();
            Map<String, Object> values = new java.util.HashMap<>();
            values.put("prompt", request.promptReference());
            values.put("cwd", request.workspaceId());
            values.put("beadIds", request.beadIds());
            values.put("buildCommands", request.buildCommands());
            values.put("testCommands", request.testCommands());
            if (backend != null) {
                values.put("providerIdentifier", backend.providerIdentifier());
                values.put("modelIdentifier", backend.modelIdentifier());
                values.put("baseUrl", backend.baseUrl());
                values.put("apiKey", backend.apiKey());
            }
            String body = mapper.writeValueAsString(values);
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(runnerUrl + "/run"))
                    .timeout(Duration.ofMinutes(31)).header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new AgentRunResult(request.idempotencyKey(), AgentRunResult.State.FAILED, null,
                        "RUNNER_HTTP_" + response.statusCode(), response.body(), List.of());
            }
            Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() { });
            List<AgentRunEvent> events = ((List<Map<String, Object>>) result.getOrDefault("events", List.of())).stream()
                    .map(event -> new AgentRunEvent(eventsIndex(event), String.valueOf(event.get("eventType")),
                            String.valueOf(event.getOrDefault("payload", "")), Boolean.TRUE.equals(event.get("redacted")),
                            java.time.Instant.parse(String.valueOf(event.get("createdAt"))))).toList();
            AgentRunResult.State state = "SUCCEEDED".equals(result.get("state"))
                    ? AgentRunResult.State.SUCCEEDED : AgentRunResult.State.FAILED;
            return new AgentRunResult(request.idempotencyKey(), state, (String) result.get("commitSha"),
                    (String) result.get("errorCode"), (String) result.get("errorMessage"), events);
        } catch (Exception exception) {
            return new AgentRunResult(request.idempotencyKey(), AgentRunResult.State.FAILED, null,
                    "RUNNER_UNAVAILABLE", exception.getMessage(), List.of());
        }
    }

    private long eventsIndex(Map<String, Object> event) {
        return event.get("sequenceNumber") instanceof Number number ? number.longValue() : 0;
    }
}
