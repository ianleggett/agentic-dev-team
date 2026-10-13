package dev.yaadt.controller.executor;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FakeAgentExecutor implements AgentExecutor {
    private final Map<String, AgentRunResult> completed = new ConcurrentHashMap<>();
    private final AtomicInteger invocations = new AtomicInteger();
    private final Clock clock;

    public FakeAgentExecutor(Clock clock) { this.clock = clock; }

    @Override
    public AgentRunResult execute(AgentRunRequest request) {
        AgentRunResult existing = completed.get(request.idempotencyKey());
        if (existing != null) return existing;
        invocations.incrementAndGet();
        Instant now = Instant.now(clock);
        AgentRunResult result = new AgentRunResult(request.idempotencyKey(), AgentRunResult.State.SUCCEEDED,
                "fake-" + request.attemptNumber(), null, null,
                List.of(new AgentRunEvent(1, "RUN_STARTED", "fake", true, now),
                        new AgentRunEvent(2, "RUN_COMPLETED", "checkpoint captured", true, now)));
        AgentRunResult raced = completed.putIfAbsent(request.idempotencyKey(), result);
        return raced == null ? result : raced;
    }

    public int invocationCount() { return invocations.get(); }
}
