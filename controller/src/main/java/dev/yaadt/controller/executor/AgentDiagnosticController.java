package dev.yaadt.controller.executor;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import dev.yaadt.controller.profile.AgentProfileVersion;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;

@RestController
@RequestMapping("/api/agent-diagnostics")
public class AgentDiagnosticController {
    private final AgentProfileVersionRepository profiles;
    private final AgentExecutor executor;

    public AgentDiagnosticController(AgentProfileVersionRepository profiles, AgentExecutor executor) {
        this.profiles = profiles;
        this.executor = executor;
    }

    @PostMapping
    public AgentDiagnosticResponse run(@RequestBody AgentDiagnosticRequest request) {
        if (request.agentProfileVersionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "agentProfileVersionId is required");
        }
        AgentProfileVersion profile = profiles.findById(request.agentProfileVersionId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent profile version not found"));
        String prompt = request.prompt() == null || request.prompt().isBlank()
                ? "Reply with exactly: hello world"
                : request.prompt();
        AgentRunResult result = executor.execute(new AgentRunRequest(
                "diagnostic:" + UUID.randomUUID(), UUID.randomUUID(), 1, "/workspace", prompt, java.util.Map.of(),
                new AgentBackendConfig(profile.getProviderIdentifier(), profile.getModelIdentifier(),
                        profile.getBaseUrl(), profile.getApiKey())));
        return new AgentDiagnosticResponse(result.state().name(), profile.getId(), profile.getProviderIdentifier(),
                profile.getModelIdentifier(), profile.getBaseUrl(), result.errorCode(), result.errorMessage(), result.events());
    }
}
