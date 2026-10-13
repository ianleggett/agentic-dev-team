package dev.yaadt.controller.sprint;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/sprints/{sprintId}")
public class SprintCommandController {
    private final SprintCommandService service;
    public SprintCommandController(SprintCommandService service) { this.service = service; }
    @GetMapping public SprintResponse get(@PathVariable UUID sprintId) { return service.get(sprintId); }
    @PostMapping("/{action}") public SprintResponse command(@PathVariable UUID sprintId, @PathVariable String action,
            @Valid @RequestBody SprintCommandRequest request, @RequestHeader(value="X-User-Subject", defaultValue="local-user") String subject,
            @RequestHeader(value="X-User-Name", defaultValue="Local User") String name) { return service.command(sprintId, action, request, subject, name); }
}
