package dev.yaadt.controller.sprint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DependencyGraphValidatorTest {
    private SprintStepRequest step(String key, List<String> dependencies) {
        return new SprintStepRequest(key, List.of("workspace-1ox.1"), "builder", UUID.randomUUID(),
                List.of(UUID.randomUUID()), "tests pass", "stop", dependencies, List.of());
    }

    @Test
    void acceptsAcyclicPlan() {
        assertDoesNotThrow(() -> DependencyGraphValidator.validate(List.of(step("build", List.of()),
                step("test", List.of("build")))));
    }

    @Test
    void rejectsCyclesAndUnknownDependencies() {
        assertThrows(IllegalArgumentException.class, () -> DependencyGraphValidator.validate(List.of(
                step("a", List.of("b")), step("b", List.of("a")))));
        assertThrows(IllegalArgumentException.class, () -> DependencyGraphValidator.validate(List.of(
                step("a", List.of("missing")))));
    }
}
