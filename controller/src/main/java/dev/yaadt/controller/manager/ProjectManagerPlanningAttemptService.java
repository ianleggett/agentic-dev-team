package dev.yaadt.controller.manager;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectManagerPlanningAttemptService {
    private final ProjectManagerPlanningAttemptRepository attempts;
    private final Clock clock;
    public ProjectManagerPlanningAttemptService(ProjectManagerPlanningAttemptRepository attempts, Clock clock) {
        this.attempts = attempts; this.clock = clock;
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID start(UUID projectId, String prompt, String provider, String model, String baseUrl) {
        return attempts.save(new ProjectManagerPlanningAttempt(projectId, prompt, provider, model, baseUrl, clock.instant())).getId();
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(UUID id, String state, String output, String errorCode, String errorMessage) {
        attempts.findById(id).ifPresent(attempt -> {
            attempt.finish(state, output, errorCode, errorMessage, clock.instant());
            attempts.save(attempt);
        });
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancelLatest(UUID projectId) {
        attempts.findByProjectIdOrderByCreatedAtDesc(projectId).stream().findFirst().ifPresent(attempt -> {
            if ("RUNNING".equals(attempt.getState())) {
                attempt.finish("CANCELLED", attempt.getModelOutput(), "PLANNING_CANCELLED", "Planning cancelled by user", clock.instant());
                attempts.save(attempt);
            }
        });
    }
    @Transactional(readOnly = true)
    public List<ProjectManagerPlanningAttemptResponse> list(UUID projectId) {
        return attempts.findByProjectIdOrderByCreatedAtDesc(projectId).stream().map(ProjectManagerPlanningAttemptResponse::from).toList();
    }
}
