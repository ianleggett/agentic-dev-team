package dev.yaadt.controller.sprint;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sprints/{sprintId}")
public class SprintCommandController {
    private final SprintCommandService service;
    private final SprintPlanService plans;
    public SprintCommandController(SprintCommandService service, SprintPlanService plans) { this.service = service; this.plans = plans; }
    @GetMapping public SprintResponse get(@PathVariable UUID sprintId) { return service.get(sprintId); }
    @GetMapping("/commands/{commandId}") public SprintCommandResponse getCommand(@PathVariable UUID commandId) {
        return service.getCommand(commandId);
    }
    @PostMapping("/{action}") @ResponseStatus(HttpStatus.ACCEPTED)
    public SprintCommandResponse command(@PathVariable UUID sprintId, @PathVariable String action,
            @Valid @RequestBody SprintCommandRequest request, @RequestHeader(value="X-User-Subject", defaultValue="local-user") String subject,
            @RequestHeader(value="X-User-Name", defaultValue="Local User") String name,
            @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        return service.submit(sprintId, action, request, subject, name, idempotencyKey);
    }
    @PostMapping("/replan") @ResponseStatus(HttpStatus.ACCEPTED)
    public SprintCommandResponse replan(@PathVariable UUID sprintId, @Valid @RequestBody SprintPlanRequest request,
            @RequestHeader(value="X-User-Subject", defaultValue="local-user") String subject,
            @RequestHeader(value="X-User-Name", defaultValue="Local User") String name,
            @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        return plans.submitReplan(sprintId, request, subject, name, idempotencyKey);
    }
}
