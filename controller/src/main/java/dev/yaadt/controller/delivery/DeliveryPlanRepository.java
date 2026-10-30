package dev.yaadt.controller.delivery;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface DeliveryPlanRepository extends JpaRepository<DeliveryPlan, UUID> {
    List<DeliveryPlan> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from DeliveryPlan p where p.id = :id")
    java.util.Optional<DeliveryPlan> findForUpdateById(@Param("id") UUID id);
}
