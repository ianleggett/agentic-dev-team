package dev.yaadt.controller.sprint;

import dev.yaadt.controller.audit.AuditEvent;
import dev.yaadt.controller.audit.AuditEventRepository;
import dev.yaadt.controller.user.CurrentUserService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class SprintCommandService {
    private final SprintRepository sprints; private final SprintPlanVersionRepository plans;
    private final AuditEventRepository audit; private final CurrentUserService users; private final Clock clock;
    private final SprintCommandIdempotencyRepository idempotency;
    private final SprintCommandRepository commands;
    private final SprintCommandOutboxRepository outbox;
    private final ObjectMapper mapper;
    private final SprintPlanService planService;
    public SprintCommandService(SprintRepository sprints, SprintPlanVersionRepository plans, AuditEventRepository audit,
            CurrentUserService users, Clock clock, SprintCommandIdempotencyRepository idempotency, SprintCommandRepository commands,
            SprintCommandOutboxRepository outbox, ObjectMapper mapper, SprintPlanService planService) {
        this.sprints = sprints; this.plans = plans; this.audit = audit; this.users = users; this.clock = clock; this.idempotency = idempotency;
        this.commands = commands; this.outbox = outbox; this.mapper = mapper; this.planService = planService;
    }
    @Transactional(readOnly = true)
    public SprintResponse get(UUID id) { Sprint sprint = find(id); SprintPlanVersion plan = plans.findTopBySprintIdOrderByVersionDesc(id).orElse(null); return new SprintResponse(
            sprint.getId(), sprint.getProjectId(), sprint.getObjective(), sprint.getState(), sprint.getCurrentPlanVersion(),
            plan == null ? null : plan.getId(), 0, sprint.getCreatedAt(), sprint.getUpdatedAt()); }
    public SprintCommandResponse submit(UUID id, String action, SprintCommandRequest request, String subject, String name, String key) {
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key is required");
        find(id);
        var prior = commands.findBySprintIdAndIdempotencyKey(id, key).orElse(null);
        if (prior != null) {
            if (!prior.getAction().equals(action) || prior.getPlanVersion() != request.planVersion() || !prior.getReason().equals(request.reason()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used for another command");
            return commandResponse(prior);
        }
        Instant now = Instant.now(clock);
        SprintCommand command = commands.save(new SprintCommand(id, key, action, request.planVersion(), request.reason(), "{}", subject, name, now));
        outbox.save(new SprintCommandOutbox(command.getId(), now));
        return commandResponse(command);
    }
    @Transactional
    public SprintCommandResponse apply(UUID commandId) {
        SprintCommand command = commands.findById(commandId).orElseThrow();
        if (!command.getStatus().equals("RECEIVED")) return commandResponse(command);
        try {
            SprintResponse result = command.getAction().equals("replan")
                    ? planService.applyReplan(command)
                    : command(command.getSprintId(), command.getAction(),
                        new SprintCommandRequest(command.getPlanVersion(), command.getReason()),
                        command.getActorSubject(), command.getActorName(), null);
            command.applied(result.state(), Instant.now(clock));
        } catch (ResponseStatusException failure) {
            command.rejected(failure.getReason(), Instant.now(clock));
        }
        return commandResponse(commands.save(command));
    }
    private SprintCommandResponse commandResponse(SprintCommand command) {
        return new SprintCommandResponse(command.getId(), command.getSprintId(), command.getAction(), command.getStatus(), command.getResultState());
    }
    @Transactional(readOnly = true)
    public SprintCommandResponse getCommand(UUID commandId) {
        return commandResponse(commands.findById(commandId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Command not found")));
    }
    public SprintResponse command(UUID id, String action, SprintCommandRequest request, String subject, String name, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var prior = idempotency.findByKey(idempotencyKey).orElse(null);
            if (prior != null) {
                if (!prior.getSprintId().equals(id) || !prior.getAction().equals(action)
                        || prior.getPlanVersion() != request.planVersion() || !prior.getReason().equals(request.reason())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used for another command");
                }
                return get(id);
            }
        }
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
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotency.save(new SprintCommandIdempotency(idempotencyKey, id, action, request.planVersion(), request.reason(), after, now));
        }
        return get(id);
    }
    private String require(String current, String expected, String next) { if (!current.equals(expected)) invalid("Invalid transition from " + current); return next; }
    private String requireNotTerminal(String current, String next) { if (current.equals("COMPLETED") || current.equals("CANCELLED")) invalid("Sprint is terminal"); return next; }
    private Sprint find(UUID id) { return sprints.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sprint not found")); }
    private void invalid(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
