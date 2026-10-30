package dev.yaadt.controller.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryPlanTest {
    @Test
    void approvalIsBoundToTheCurrentVersion() {
        Instant now = Instant.parse("2026-10-30T00:00:00Z");
        DeliveryPlan plan = new DeliveryPlan(UUID.randomUUID(), "durable delivery", UUID.randomUUID(), now);

        plan.submitForApproval(now.plusSeconds(1));
        plan.approve(1, "operator", "approved for the next milestone", now.plusSeconds(2));

        assertThat(plan.getState()).isEqualTo("APPROVED");
        assertThat(plan.getApprovedVersion()).isEqualTo(1);
        assertThat(plan.getApprovedBy()).isEqualTo("operator");
        assertThat(plan.getApprovalReason()).isEqualTo("approved for the next milestone");
    }

    @Test
    void staleVersionCannotBeApproved() {
        DeliveryPlan plan = new DeliveryPlan(UUID.randomUUID(), "durable delivery", UUID.randomUUID(), Instant.now());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> plan.approve(2, "operator", "stale", Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revisionUpdatesThePlanObjectiveAndClearsApproval() {
        Instant now = Instant.parse("2026-10-30T00:00:00Z");
        DeliveryPlan plan = new DeliveryPlan(UUID.randomUUID(), "initial objective", UUID.randomUUID(), now);
        plan.submitForApproval(now.plusSeconds(1));
        plan.approve(1, "operator", "approved", now.plusSeconds(2));

        int version = plan.revise("revised objective", now.plusSeconds(3));

        assertThat(version).isEqualTo(2);
        assertThat(plan.getObjective()).isEqualTo("revised objective");
        assertThat(plan.getState()).isEqualTo("DRAFT");
        assertThat(plan.getApprovedVersion()).isNull();
    }
}
