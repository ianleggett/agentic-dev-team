package dev.yaadt.controller.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    List<Repository> findByProjectIdOrderByName(UUID projectId);
}
