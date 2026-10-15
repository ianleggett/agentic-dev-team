package dev.yaadt.controller.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sprints/{sprintId}/events")
public class AuditEventController {
    private final AuditEventRepository events;

    public AuditEventController(AuditEventRepository events) {
        this.events = events;
    }

    @GetMapping
    public List<AuditEventResponse> list(@PathVariable UUID sprintId) {
        return events.findBySprintIdOrderByCreatedAtAsc(sprintId).stream().map(AuditEventResponse::from).toList();
    }
}
