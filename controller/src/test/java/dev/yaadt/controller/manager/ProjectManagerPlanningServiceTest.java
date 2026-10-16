package dev.yaadt.controller.manager;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ProjectManagerPlanningServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void rejectsPlansWithoutSteps() throws Exception {
        var plan = mapper.readTree("{\"objective\":\"ship it\",\"steps\":[]}");
        assertThatIllegalArgumentException().isThrownBy(() -> ProjectManagerPlanningService.validateGeneratedPlan(plan))
                .withMessage("generated plan contains no steps");
    }

    @Test
    void requiresEachStepToReferenceBeads() throws Exception {
        var plan = mapper.readTree("{\"objective\":\"ship it\",\"steps\":[{\"stepKey\":\"build\",\"beadIds\":[]}]}");
        assertThatIllegalArgumentException().isThrownBy(() -> ProjectManagerPlanningService.validateGeneratedPlan(plan))
                .withMessage("generated plan step has no Beads: build");
    }

    @Test
    void acceptsABeadBackedStep() throws Exception {
        var plan = mapper.readTree("{\"objective\":\"ship it\",\"steps\":[{\"stepKey\":\"build\",\"beadIds\":[\"bd-1\"]}]}");
        assertThatCode(() -> ProjectManagerPlanningService.validateGeneratedPlan(plan)).doesNotThrowAnyException();
    }
}
