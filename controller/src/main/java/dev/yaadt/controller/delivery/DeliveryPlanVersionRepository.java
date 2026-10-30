package dev.yaadt.controller.delivery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryPlanVersionRepository extends JpaRepository<DeliveryPlanVersion, UUID> {
    List<DeliveryPlanVersion> findByDeliveryPlanIdOrderByVersionDesc(UUID deliveryPlanId);
    Optional<DeliveryPlanVersion> findByDeliveryPlanIdAndVersion(UUID deliveryPlanId, int version);
}
