package dev.yaadt.controller.delivery;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPlanBeadRepository extends JpaRepository<DeliveryPlanBead, UUID> {
    List<DeliveryPlanBead> findByDeliveryPlanVersionIdOrderByBeadId(UUID deliveryPlanVersionId);
}
