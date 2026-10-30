package dev.yaadt.controller.delivery;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryMilestoneRepository extends JpaRepository<DeliveryMilestone, UUID> {
    List<DeliveryMilestone> findByDeliveryPlanVersionIdOrderBySequenceNumber(UUID deliveryPlanVersionId);
}
