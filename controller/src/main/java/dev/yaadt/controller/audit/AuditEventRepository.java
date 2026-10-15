package dev.yaadt.controller.audit;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findBySprintIdOrderByCreatedAtAsc(UUID sprintId);
}
