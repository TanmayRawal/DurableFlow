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

@Entity
@Table(name = "task_executions")
public class TaskExecutionEntity {
    @Id private UUID id;
    @Column(name = "workflow_run_id", nullable = false) private UUID workflowRunId;
    @Column(name = "node_key", nullable = false) private String nodeKey;
    @Column(name = "handler_type", nullable = false) private String handlerType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private TaskStatus status;
    @Column(nullable = false) private int attempt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "lease_owner") private String leaseOwner;
    @Column(name = "lease_expires_at") private Instant leaseExpiresAt;
    @Column(name = "last_heartbeat_at") private Instant lastHeartbeatAt;
    @Column(name = "failure_reason") private String failureReason;
    @Version @Column(name = "record_version") private long recordVersion;

    protected TaskExecutionEntity() { }
    public TaskExecutionEntity(UUID id, UUID workflowRunId, String nodeKey, String handlerType, TaskStatus status, Instant now) {
        this.id = id; this.workflowRunId = workflowRunId; this.nodeKey = nodeKey; this.handlerType = handlerType;
        this.status = status; this.attempt = 0; this.maxAttempts = 3; this.createdAt = now; this.updatedAt = now;
    }
    public UUID getId() { return id; }
    public UUID getWorkflowRunId() { return workflowRunId; }
    public String getNodeKey() { return nodeKey; }
    public String getHandlerType() { return handlerType; }
    public TaskStatus getStatus() { return status; }
    public int getAttempt() { return attempt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getLeaseOwner() { return leaseOwner; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getFailureReason() { return failureReason; }
    public void claim(String workerId, Instant now, java.time.Duration leaseDuration) {
        require(TaskStatus.READY, "Only READY tasks can be claimed");
        status = TaskStatus.RUNNING; attempt++; leaseOwner = workerId; leaseExpiresAt = now.plus(leaseDuration); lastHeartbeatAt = now; updatedAt = now;
    }
    public void heartbeat(String workerId, Instant now, java.time.Duration leaseDuration) {
        require(TaskStatus.RUNNING, "Only RUNNING tasks can receive a heartbeat"); requireOwner(workerId);
        leaseExpiresAt = now.plus(leaseDuration); lastHeartbeatAt = now; updatedAt = now;
    }
    public void complete(String workerId, Instant now) { require(TaskStatus.RUNNING, "Only RUNNING tasks can be completed"); requireOwner(workerId); status = TaskStatus.SUCCEEDED; clearLease(); updatedAt = now; }
    public void fail(String reason, Instant now) {
        require(TaskStatus.RUNNING, "Only RUNNING tasks can fail"); failureReason = reason; clearLease(); updatedAt = now;
        if (attempt < maxAttempts) { status = TaskStatus.RETRYING; nextAttemptAt = now.plusSeconds(backoffSeconds()); }
        else { status = TaskStatus.DEAD_LETTER; }
    }
    public void makeReadyFromRetry(Instant now) { require(TaskStatus.RETRYING, "Only RETRYING tasks can be scheduled"); status = TaskStatus.READY; nextAttemptAt = null; updatedAt = now; }
    public void makeReady(Instant now) { require(TaskStatus.PENDING, "Only PENDING tasks can become ready"); status = TaskStatus.READY; updatedAt = now; }
    private long backoffSeconds() { return Math.min(300, 5L * (1L << Math.max(0, attempt - 1))); }
    private void clearLease() { leaseOwner = null; leaseExpiresAt = null; }
    private void requireOwner(String workerId) { if (!workerId.equals(leaseOwner)) throw new InvalidTaskTransitionException("Task lease is owned by a different worker"); }
    private void require(TaskStatus expected, String message) { if (status != expected) throw new InvalidTaskTransitionException(message + "; current status is " + status); }
}
