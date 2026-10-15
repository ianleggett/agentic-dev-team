package dev.yaadt.controller.sprint;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/projects/{projectId}/sprints")
public class SprintStreamController {
    private static final ScheduledExecutorService STREAMS = Executors.newScheduledThreadPool(2);
    private final SprintRepository sprints;

    public SprintStreamController(SprintRepository sprints) { this.sprints = sprints; }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID projectId) {
        SseEmitter emitter = new SseEmitter(0L);
        ScheduledFuture<?> task = STREAMS.scheduleAtFixedRate(() -> {
            try {
                List<SprintStateUpdate> updates = sprints.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                        .map(sprint -> new SprintStateUpdate(sprint.getId(), sprint.getState(), sprint.getUpdatedAt())).toList();
                emitter.send(SseEmitter.event().name("sprint-update").data(updates));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
            }
        }, 0, 2, TimeUnit.SECONDS);
        Runnable cleanup = () -> task.cancel(false);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());
        return emitter;
    }
}
