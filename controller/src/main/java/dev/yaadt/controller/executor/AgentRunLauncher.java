package dev.yaadt.controller.executor;

import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AgentRunLauncher {
    private final AgentRunService service;

    public AgentRunLauncher(AgentRunService service) {
        this.service = service;
    }

    @Async
    public void execute(UUID runId) {
        service.executeRun(runId);
    }
}
