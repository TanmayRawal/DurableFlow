package io.tanmayrawal.durableflow.dispatch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event_outbox")
public class OutboxEventEntity {
    @Id private UUID id;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb") private String payloadJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "publish_attempts", nullable = false) private int publishAttempts;
    @Version @Column(name = "record_version") private long recordVersion;

    protected OutboxEventEntity() { }
    public OutboxEventEntity(UUID id, String eventType, UUID aggregateId, String payloadJson, Instant createdAt) {
        this.id = id; this.eventType = eventType; this.aggregateId = aggregateId; this.payloadJson = payloadJson; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getEventType() { return eventType; }
    public UUID getAggregateId() { return aggregateId; }
    public String getPayloadJson() { return payloadJson; }
    public void markPublished(Instant now) { publishedAt = now; publishAttempts++; }
    public void recordPublishAttempt() { publishAttempts++; }
}
