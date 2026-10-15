package dev.yaadt.controller.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RunnerAdminClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper;
    private final String runnerUrl;
    private final String token;
    public RunnerAdminClient(ObjectMapper mapper, @Value("${yaadt.agent.runner-url:http://agent-runner:3010}") String runnerUrl,
            @Value("${yaadt.agent.runner-token:}") String token) {
        this.mapper = mapper; this.runnerUrl = runnerUrl; this.token = token;
    }
    public Map<String, Object> resetBeadsOwnership() { return post("/admin/beads/reset-ownership", Map.of()); }
    public Map<String, Object> reindexCocoIndex(String workspacePath) { return post("/admin/cocoindex/reindex", Map.of("workspacePath", workspacePath)); }
    private Map<String, Object> post(String path, Map<String, String> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(runnerUrl + path)).timeout(Duration.ofMinutes(31))
                    .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, response.body());
            return mapper.readValue(response.body(), Map.class);
        } catch (ResponseStatusException exception) { throw exception; }
        catch (Exception exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Agent runner operation failed", exception); }
    }
}
