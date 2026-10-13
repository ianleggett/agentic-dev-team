package dev.yaadt.controller.temporal;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "yaadt.temporal")
public record TemporalProperties(boolean workerEnabled, String target, String namespace, String taskQueue) {
    public TemporalProperties {
        target = target == null || target.isBlank() ? "localhost:7233" : target;
        namespace = namespace == null || namespace.isBlank() ? "default" : namespace;
        taskQueue = taskQueue == null || taskQueue.isBlank() ? "yaadt" : taskQueue;
    }
}
