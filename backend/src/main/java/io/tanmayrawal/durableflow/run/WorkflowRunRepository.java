package io.tanmayrawal.durableflow.run;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface WorkflowRunRepository extends JpaRepository<WorkflowRunEntity, UUID> {
    Optional<WorkflowRunEntity> findByWorkflowDefinitionIdAndIdempotencyKey(UUID workflowDefinitionId, String idempotencyKey);
}
