package dev.yaadt.controller.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProjectManagerPlanningServiceTest {
    @Test
    void fallsBackToApprovedPlanObjectiveWhenManagerObjectiveIsMissing() {
        assertThat(ProjectManagerPlanningService.resolveSprintObjective("", "Build the shopping list", "Project directive", "Project objective"))
                .isEqualTo("Build the shopping list");
    }

    @Test
    void fallsBackToDirectiveWhenNoPlanExists() {
        assertThat(ProjectManagerPlanningService.resolveSprintObjective("", "", "Build the shopping list", "Project objective"))
                .isEqualTo("Build the shopping list");
    }

    @Test
    void rejectsProjectsWithoutAnyObjective() {
        assertThatThrownBy(() -> ProjectManagerPlanningService.resolveSprintObjective("", "", "", " "))
                .hasMessageContaining("sprint objective is blank");
    }
}
