package dev.yaadt.controller.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FakeAgentExecutorTest {
    @Test
    void duplicateIdempotencyKeyReturnsTheOriginalAttempt() {
        FakeAgentExecutor executor = new FakeAgentExecutor(Clock.systemUTC());
        AgentRunRequest request = new AgentRunRequest("run-1", UUID.randomUUID(), 1, "ws", "prompt", Map.of());
        AgentRunResult first = executor.execute(request);
        AgentRunResult duplicate = executor.execute(request);
        assertSame(first, duplicate);
        assertEquals(1, executor.invocationCount());
        assertEquals(List.of(1L, 2L), first.events().stream().map(AgentRunEvent::sequenceNumber).toList());
    }
}
