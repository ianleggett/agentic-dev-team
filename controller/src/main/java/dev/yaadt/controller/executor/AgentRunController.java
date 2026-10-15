package dev.yaadt.controller.executor;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AgentRunController {
    private final AgentRunService service;
    private final AgentRunLauncher launcher;
    public AgentRunController(AgentRunService service, AgentRunLauncher launcher) {
        this.service = service; this.launcher = launcher;
    }
    @PostMapping("/steps/{stepId}/runs")
    public AgentRunResponse execute(@PathVariable UUID stepId) {
        AgentRunResponse run = service.prepareRun(stepId);
        launcher.execute(run.id());
        return run;
    }
    @GetMapping("/agent-runs/{runId}")
    public AgentRunResponse get(@PathVariable UUID runId) { return service.get(runId); }
    @GetMapping("/agent-runs/{runId}/events")
    public List<RunEventResponse> events(@PathVariable UUID runId) { return service.events(runId); }
    @GetMapping("/steps/{stepId}/runs")
    public List<AgentRunResponse> runs(@PathVariable UUID stepId) { return service.list(stepId); }
}
