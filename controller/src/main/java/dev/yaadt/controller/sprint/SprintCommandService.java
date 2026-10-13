package dev.yaadt.controller.sprint;

import dev.yaadt.controller.audit.AuditEvent;
import dev.yaadt.controller.audit.AuditEventRepository;
import dev.yaadt.controller.user.CurrentUserService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class SprintCommandService {
    private final SprintRepository sprints; private final SprintPlanVersionRepository plans;
    private final AuditEventRepository audit; private final CurrentUserService users; private final Clock clock;
    public SprintCommandService(SprintRepository sprints, SprintPlanVersionRepository plans, AuditEventRepository audit,
            CurrentUserService users, Clock clock) { this.sprints = sprints; this.plans = plans; this.audit = audit; this.users = users; this.clock = clock; }
    @Transactional(readOnly = true)
    public SprintResponse get(UUID id) { Sprint sprint = find(id); SprintPlanVersion plan = plans.findTopBySprintIdOrderByVersionDesc(id).orElse(null); return new SprintResponse(
            sprint.getId(), sprint.getProjectId(), sprint.getObjective(), sprint.getState(), sprint.getCurrentPlanVersion(),
            plan == null ? null : plan.getId(), 0, sprint.getCreatedAt(), sprint.getUpdatedAt()); }
    public SprintResponse command(UUID id, String action, SprintCommandRequest request, String subject, String name) {
        Sprint sprint = find(id); if (!sprint.getState().equals("DRAFT") && action.equals("approve")) invalid("Sprint is not awaiting approval");
        SprintPlanVersion plan = plans.findTopBySprintIdOrderByVersionDesc(id).orElse(null);
        if (plan != null && request.planVersion() != plan.getVersion()) invalid("Plan version is stale");
        String before = sprint.getState(); String after = switch (action) {
            case "approve" -> "READY"; case "reject" -> "FAILED"; case "pause" -> require(before, "RUNNING", "PAUSED");
            case "resume" -> require(before, "PAUSED", "READY"); case "retry" -> require(before, "FAILED", "READY");
            case "cancel" -> requireNotTerminal(before, "CANCELLED"); case "skip" -> require(before, "READY", "READY");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown sprint command"); };
        Instant now = Instant.now(clock); sprint.transition(after, now);
        String actor = users.resolve(subject, name).getSubject(); audit.save(new AuditEvent(actor, id, action, before, after, request.reason(), now));
        return get(id);
    }
    private String require(String current, String expected, String next) { if (!current.equals(expected)) invalid("Invalid transition from " + current); return next; }
    private String requireNotTerminal(String current, String next) { if (current.equals("COMPLETED") || current.equals("CANCELLED")) invalid("Sprint is terminal"); return next; }
    private Sprint find(UUID id) { return sprints.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found")); }
    private void invalid(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
