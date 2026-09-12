package io.tanmayrawal.durableflow.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_runs")
public class WorkflowRunEntity {
    @Id private UUID id;
    @Column(name = "workflow_definition_id", nullable = false) private UUID workflowDefinitionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private WorkflowRunStatus status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_json", nullable = false, columnDefinition = "jsonb") private String inputJson;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Version @Column(name = "record_version") private long recordVersion;

    protected WorkflowRunEntity() { }
    public WorkflowRunEntity(UUID id, UUID workflowDefinitionId, String inputJson, String idempotencyKey, Instant now) {
        this.id = id; this.workflowDefinitionId = workflowDefinitionId; this.inputJson = inputJson;
        this.idempotencyKey = idempotencyKey;
        this.status = WorkflowRunStatus.RUNNING; this.createdAt = now; this.startedAt = now;
    }
    public UUID getId() { return id; }
    public UUID getWorkflowDefinitionId() { return workflowDefinitionId; }
    public WorkflowRunStatus getStatus() { return status; }
    public String getInputJson() { return inputJson; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void succeed(Instant now) { this.status = WorkflowRunStatus.SUCCEEDED; this.completedAt = now; }
}
