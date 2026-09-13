package io.tanmayrawal.durableflow.workflow;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinitionEntity, UUID> {
    java.util.Optional<WorkflowDefinitionEntity> findTopByNameOrderByVersionDesc(String name);
}
