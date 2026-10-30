package dev.yaadt.controller.delivery;

import dev.yaadt.controller.sprint.SprintResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery-plans")
public class DeliveryPlanSchedulerController {
    private final DeliveryPlanSchedulerService scheduler;

    public DeliveryPlanSchedulerController(DeliveryPlanSchedulerService scheduler) { this.scheduler = scheduler; }

    @PostMapping("/{planId}/next-sprint")
    public SprintResponse generateNextSprint(@PathVariable UUID planId,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        return scheduler.generateNextSprint(planId, subject, displayName);
    }
}
