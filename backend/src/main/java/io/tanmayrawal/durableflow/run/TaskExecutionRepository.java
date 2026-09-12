package io.tanmayrawal.durableflow.run;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskExecutionRepository extends JpaRepository<TaskExecutionEntity, UUID> {
    List<TaskExecutionEntity> findByWorkflowRunId(UUID workflowRunId);
    List<TaskExecutionEntity> findByStatusAndLeaseExpiresAtBefore(TaskStatus status, Instant now);
    List<TaskExecutionEntity> findByStatusAndNextAttemptAtLessThanEqual(TaskStatus status, Instant now);
}
