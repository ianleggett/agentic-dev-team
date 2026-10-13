package dev.yaadt.controller.sprint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DependencyGraphValidator {
    private DependencyGraphValidator() { }

    static void validate(List<SprintStepRequest> steps) {
        Set<String> keys = new HashSet<>();
        Map<String, Set<String>> graph = new HashMap<>();
        for (SprintStepRequest step : steps) {
            if (!keys.add(step.stepKey())) throw new IllegalArgumentException("Duplicate step key");
            graph.put(step.stepKey(), new HashSet<>(step.dependsOn()));
        }
        for (Set<String> deps : graph.values()) if (!keys.containsAll(deps)) {
            throw new IllegalArgumentException("Unknown step dependency");
        }
        for (String key : keys) if (hasCycle(key, graph, new HashSet<>(), new HashSet<>())) {
            throw new IllegalArgumentException("Dependency cycle detected");
        }
    }

    private static boolean hasCycle(String key, Map<String, Set<String>> graph,
            Set<String> visiting, Set<String> visited) {
        if (visiting.contains(key)) return true;
        if (visited.contains(key)) return false;
        visiting.add(key);
        for (String dependency : graph.getOrDefault(key, Set.of())) {
            if (hasCycle(dependency, graph, visiting, visited)) return true;
        }
        visiting.remove(key);
        visited.add(key);
        return false;
    }
}
