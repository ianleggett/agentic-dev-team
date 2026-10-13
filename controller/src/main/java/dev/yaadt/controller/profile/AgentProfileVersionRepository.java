package dev.yaadt.controller.profile;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentProfileVersionRepository extends JpaRepository<AgentProfileVersion, UUID> {
    Optional<AgentProfileVersion> findTopByProfileIdOrderByVersionDesc(UUID profileId);
}
