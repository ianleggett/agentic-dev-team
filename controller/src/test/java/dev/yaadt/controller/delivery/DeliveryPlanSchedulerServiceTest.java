package dev.yaadt.controller.delivery;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yaadt.controller.beads.BeadsClient;
import dev.yaadt.controller.beads.BeadsIssue;
import dev.yaadt.controller.profile.AgentProfile;
import dev.yaadt.controller.profile.AgentProfileRepository;
import dev.yaadt.controller.profile.AgentProfileVersion;
import dev.yaadt.controller.profile.AgentProfileVersionRepository;
import dev.yaadt.controller.project.Project;
import dev.yaadt.controller.project.ProjectRepository;
import dev.yaadt.controller.sprint.Sprint;
import dev.yaadt.controller.sprint.SprintPlanService;
import dev.yaadt.controller.sprint.SprintRepository;
import dev.yaadt.controller.sprint.SprintResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryPlanSchedulerServiceTest {
    @Mock DeliveryPlanRepository plans;
    @Mock DeliveryPlanVersionRepository versions;
    @Mock SprintRepository sprints;
    @Mock SprintPlanService sprintPlans;
    @Mock ProjectRepository projects;
    @Mock AgentProfileRepository profiles;
    @Mock AgentProfileVersionRepository profileVersions;
    @Mock SprintBeadSnapshotRepository beadSnapshots;
    @Mock BeadsClient beads;

    private DeliveryPlanSchedulerService scheduler;
    private final UUID planId = UUID.randomUUID();
    private final UUID versionId = UUID.randomUUID();
    private final UUID directiveId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-30T00:00:00Z");

    @BeforeEach
    void setUp() {
        scheduler = new DeliveryPlanSchedulerService(plans, versions, sprints, sprintPlans, projects, profiles,
                profileVersions, beadSnapshots, beads, new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void generatesOnlyReadyNonTerminalWork() {
        DeliveryPlan plan = plan("APPROVED");
        DeliveryPlanVersion version = version(snapshot("b1", "fullstack", List.of()));
        Project project = new Project("YAADT", "objective", "yaadt", "local");
        AgentProfile profile = mock(AgentProfile.class);
        AgentProfileVersion profileVersion = mock(AgentProfileVersion.class);
        SprintResponse response = new SprintResponse(UUID.randomUUID(), UUID.randomUUID(), "objective", "DRAFT", 1, UUID.randomUUID(), 1, now, now);

        when(plans.findForUpdateById(planId)).thenReturn(Optional.of(plan));
        when(sprints.findByDeliveryPlanIdOrderByCreatedAtDesc(planId)).thenReturn(List.of());
        when(versions.findByDeliveryPlanIdAndVersion(planId, 1)).thenReturn(Optional.of(version));
        when(projects.findById(plan.getProjectId())).thenReturn(Optional.of(project));
        when(beads.listRelevantIssues("project:yaadt")).thenReturn(List.of(new BeadsIssue("b1", "Build", "", "open", List.of("project:yaadt"), List.of())));
        when(profiles.findByRole("fullstack")).thenReturn(Optional.of(profile));
        when(profile.getId()).thenReturn(UUID.randomUUID());
        when(profileVersions.findTopByProfileIdOrderByVersionDesc(profile.getId())).thenReturn(Optional.of(profileVersion));
        when(sprintPlans.create(any(), any(), any(), any())).thenReturn(response);
        when(sprints.findById(response.id())).thenReturn(Optional.of(new Sprint(UUID.randomUUID(), "objective", UUID.randomUUID(), now)));

        scheduler.generateNextSprint(planId, "operator", "Operator");

        verify(sprintPlans).create(any(), any(), any(), any());
    }

    @Test
    void refusesStaleBeads() {
        DeliveryPlan plan = plan("APPROVED");
        DeliveryPlanVersion version = version(snapshot("b1", "fullstack", List.of()));
        Project project = new Project("YAADT", "objective", "yaadt", "local");
        when(plans.findForUpdateById(planId)).thenReturn(Optional.of(plan));
        when(sprints.findByDeliveryPlanIdOrderByCreatedAtDesc(planId)).thenReturn(List.of());
        when(versions.findByDeliveryPlanIdAndVersion(planId, 1)).thenReturn(Optional.of(version));
        when(projects.findById(plan.getProjectId())).thenReturn(Optional.of(project));
        when(beads.listRelevantIssues("project:yaadt")).thenReturn(List.of(new BeadsIssue("b1", "Build", "", "closed", List.of("project:yaadt"), List.of())));

        assertThatThrownBy(() -> scheduler.generateNextSprint(planId, "operator", "Operator"))
                .hasMessageContaining("No ready Beads");
    }

    @Test
    void refusesWhenDependencyIsMissingFromLiveBeads() {
        DeliveryPlan plan = plan("APPROVED");
        DeliveryPlanVersion version = version("{\"approvalPolicy\":\"human\",\"failurePolicy\":\"stop\",\"steps\":["
                + "{\"stepKey\":\"dependency\",\"beadIds\":[\"missing\"],\"agentRole\":\"fullstack\",\"repositoryIds\":[],\"successCriteria\":\"tests pass\",\"failurePolicy\":\"stop\",\"dependsOn\":[],\"resourceLocks\":[]},"
                + "{\"stepKey\":\"build\",\"beadIds\":[\"b1\"],\"agentRole\":\"fullstack\",\"repositoryIds\":[],\"successCriteria\":\"tests pass\",\"failurePolicy\":\"stop\",\"dependsOn\":[\"dependency\"],\"resourceLocks\":[]}]}" );
        Project project = new Project("YAADT", "objective", "yaadt", "local");
        when(plans.findForUpdateById(planId)).thenReturn(Optional.of(plan));
        when(sprints.findByDeliveryPlanIdOrderByCreatedAtDesc(planId)).thenReturn(List.of());
        when(versions.findByDeliveryPlanIdAndVersion(planId, 1)).thenReturn(Optional.of(version));
        when(projects.findById(plan.getProjectId())).thenReturn(Optional.of(project));
        when(beads.listRelevantIssues("project:yaadt")).thenReturn(List.of(new BeadsIssue("b1", "Build", "", "open", List.of("project:yaadt"), List.of())));

        assertThatThrownBy(() -> scheduler.generateNextSprint(planId, "operator", "Operator"))
                .hasMessageContaining("No ready Beads");
    }

    @Test
    void refusesWhenPlanAlreadyHasAnActiveSprint() {
        DeliveryPlan plan = plan("APPROVED");
        when(plans.findForUpdateById(planId)).thenReturn(Optional.of(plan));
        when(sprints.findByDeliveryPlanIdOrderByCreatedAtDesc(planId))
                .thenReturn(List.of(new Sprint(UUID.randomUUID(), "objective", UUID.randomUUID(), now)));

        assertThatThrownBy(() -> scheduler.generateNextSprint(planId, "operator", "Operator"))
                .hasMessageContaining("already has an active Sprint");
    }

    private DeliveryPlan plan(String state) {
        DeliveryPlan plan = new DeliveryPlan(UUID.randomUUID(), "objective", UUID.randomUUID(), now);
        set(plan, "id", planId);
        if ("APPROVED".equals(state)) plan.approve(1, "operator", "approved", now);
        return plan;
    }

    private DeliveryPlanVersion version(String snapshot) {
        DeliveryPlanVersion version = new DeliveryPlanVersion(planId, 1, directiveId, "objective", "reasoning", "criteria", "boundaries", snapshot, "operator", now);
        set(version, "id", versionId);
        return version;
    }

    private String snapshot(String beadId, String role, List<String> dependsOn) {
        return "{\"approvalPolicy\":\"human\",\"failurePolicy\":\"stop\",\"steps\":[{\"stepKey\":\"build\",\"beadIds\":[\""
                + beadId + "\"],\"agentRole\":\"" + role + "\",\"repositoryIds\":[],\"successCriteria\":\"tests pass\",\"failurePolicy\":\"stop\",\"dependsOn\":"
                + dependsOn.toString().replace('=', ':') + ",\"resourceLocks\":[]}]}";
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
