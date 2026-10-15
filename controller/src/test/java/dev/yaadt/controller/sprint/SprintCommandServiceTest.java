package dev.yaadt.controller.sprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.yaadt.controller.audit.AuditEventRepository;
import dev.yaadt.controller.user.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SprintCommandServiceTest {
    @Mock SprintRepository sprints;
    @Mock SprintPlanVersionRepository plans;
    @Mock AuditEventRepository audit;
    @Mock CurrentUserService users;
    @Mock SprintCommandIdempotencyRepository legacyIdempotency;
    @Mock SprintCommandRepository commands;
    @Mock SprintCommandOutboxRepository outbox;
    @Mock SprintPlanService planService;
    private SprintCommandService service;
    private UUID sprintId;

    @BeforeEach
    void setUp() {
        service = new SprintCommandService(sprints, plans, audit, users,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), legacyIdempotency,
                commands, outbox, new ObjectMapper(), planService);
        sprintId = UUID.randomUUID();
        when(sprints.findById(sprintId)).thenReturn(Optional.of(new Sprint(UUID.randomUUID(), "objective", UUID.randomUUID(), Instant.now())));
    }

    @Test
    void duplicateKeyReturnsOriginalCommandWithoutCreatingAnotherOutboxEntry() {
        SprintCommand prior = new SprintCommand(sprintId, "same-key", "approve", 1, "approve", "{}", "user", "User", Instant.now());
        when(commands.findBySprintIdAndIdempotencyKey(sprintId, "same-key")).thenReturn(Optional.of(prior));

        SprintCommandResponse response = service.submit(sprintId, "approve", new SprintCommandRequest(1, "approve"), "user", "User", "same-key");

        assertThat(response.commandId()).isEqualTo(prior.getId());
        assertThat(response.status()).isEqualTo("RECEIVED");
    }

    @Test
    void reusingKeyForDifferentCommandIsRejected() {
        SprintCommand prior = new SprintCommand(sprintId, "same-key", "approve", 1, "approve", "{}", "user", "User", Instant.now());
        when(commands.findBySprintIdAndIdempotencyKey(sprintId, "same-key")).thenReturn(Optional.of(prior));

        assertThatThrownBy(() -> service.submit(sprintId, "pause", new SprintCommandRequest(1, "pause"), "user", "User", "same-key"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already used");
    }
}
