package dev.yaadt.controller.delivery;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import dev.yaadt.controller.sprint.SprintResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DeliveryPlanController {
    private final DeliveryPlanService service;
    private final DeliveryPlanSprintQueryService sprintQuery;
    private final DeliveryPlanBeadLiveQueryService liveBeadQuery;

    public DeliveryPlanController(DeliveryPlanService service, DeliveryPlanSprintQueryService sprintQuery,
            DeliveryPlanBeadLiveQueryService liveBeadQuery) {
        this.service = service; this.sprintQuery = sprintQuery; this.liveBeadQuery = liveBeadQuery;
    }

    @GetMapping("/projects/{projectId}/delivery-plans")
    public List<DeliveryPlanResponse> list(@PathVariable UUID projectId) { return service.list(projectId); }

    @PostMapping("/projects/{projectId}/delivery-plans")
    public ResponseEntity<DeliveryPlanResponse> create(@PathVariable UUID projectId, @Valid @RequestBody DeliveryPlanRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject,
            @RequestHeader(value = "X-User-Name", defaultValue = "Local User") String displayName) {
        DeliveryPlanResponse response = service.create(projectId, request, subject, displayName);
        return ResponseEntity.created(URI.create("/api/delivery-plans/" + response.id())).body(response);
    }

    @GetMapping("/delivery-plans/{planId}")
    public DeliveryPlanResponse get(@PathVariable UUID planId) { return service.get(planId); }

    @GetMapping("/delivery-plans/{planId}/beads")
    public List<DeliveryPlanBeadResponse> beads(@PathVariable UUID planId) { return service.beads(planId); }

    @GetMapping("/delivery-plans/{planId}/beads/live")
    public List<DeliveryPlanBeadLiveResponse> liveBeads(@PathVariable UUID planId) { return liveBeadQuery.list(planId); }

    @GetMapping("/delivery-plans/{planId}/sprints")
    public List<SprintResponse> sprints(@PathVariable UUID planId) { return sprintQuery.list(planId); }

    @PostMapping("/delivery-plans/{planId}/versions")
    public DeliveryPlanResponse revise(@PathVariable UUID planId, @Valid @RequestBody DeliveryPlanRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject) {
        return service.revise(planId, request, subject);
    }

    @PostMapping("/delivery-plans/{planId}/submit")
    public DeliveryPlanResponse submit(@PathVariable UUID planId) { return service.submit(planId); }

    @PostMapping("/delivery-plans/{planId}/approve")
    public DeliveryPlanResponse approve(@PathVariable UUID planId, @Valid @RequestBody DeliveryPlanDecisionRequest request,
            @RequestHeader(value = "X-User-Subject", defaultValue = "local-user") String subject) {
        return service.approve(planId, request, subject);
    }

    @PostMapping("/delivery-plans/{planId}/reject")
    public DeliveryPlanResponse reject(@PathVariable UUID planId) { return service.reject(planId); }
}
