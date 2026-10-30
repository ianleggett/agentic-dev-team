package dev.yaadt.controller.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.user.CurrentUserService;
import dev.yaadt.controller.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanServiceTest {
    @Mock DeliveryPlanRepository plans;
    @Mock DeliveryPlanVersionRepository versions;
    @Mock DeliveryMilestoneRepository milestones;
    @Mock DeliveryPlanBeadRepository planBeads;
    @Mock ProjectRepository projects;
    @Mock CurrentUserService users;

    private DeliveryPlanService service;
    private final UUID projectId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID directiveId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new DeliveryPlanService(plans, versions, milestones, planBeads, projects, users,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void createPersistsTheInitialImmutableVersion() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(projects.existsById(projectId)).thenReturn(true);
        when(users.resolve("operator", "Operator")).thenReturn(user);
        when(plans.save(any(DeliveryPlan.class))).thenAnswer(invocation -> {
            DeliveryPlan plan = invocation.getArgument(0);
            set(plan, "id", planId);
            return plan;
        });
        when(versions.save(any(DeliveryPlanVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versions.findByDeliveryPlanIdAndVersion(planId, 1)).thenReturn(Optional.empty());

        service.create(projectId, request("initial objective"), "operator", "Operator");

        ArgumentCaptor<DeliveryPlanVersion> captured = ArgumentCaptor.forClass(DeliveryPlanVersion.class);
        verify(versions).save(captured.capture());
        assertThat(captured.getValue().getDeliveryPlanId()).isEqualTo(planId);
        assertThat(captured.getValue().getVersion()).isEqualTo(1);
        assertThat(captured.getValue().getObjective()).isEqualTo("initial objective");
    }

    @Test
    void approveRequiresPendingApprovalAndCurrentVersion() {
        DeliveryPlan plan = new DeliveryPlan(projectId, "objective", UUID.randomUUID(), now);
        set(plan, "id", planId);
        when(plans.findForUpdateById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.approve(planId, new DeliveryPlanDecisionRequest(1, "approved"), "operator"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not awaiting approval");
        verify(plans, never()).save(any(DeliveryPlan.class));
    }

    private DeliveryPlanRequest request(String objective) {
        return new DeliveryPlanRequest(objective, "reasoning", "criteria", "one sprint", "{}", List.of(),
                List.of(new DeliveryPlanBeadRequest("b1", null, "Build", "open", "[]", "[]", "{}")), directiveId);
    }

    private void set(Object target, String field, Object value) {
        try {
            var fieldRef = target.getClass().getDeclaredField(field);
            fieldRef.setAccessible(true);
            fieldRef.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
