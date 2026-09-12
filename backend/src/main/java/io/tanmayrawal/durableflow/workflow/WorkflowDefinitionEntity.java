package io.tanmayrawal.durableflow.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_definitions")
public class WorkflowDefinitionEntity {
    @Id private UUID id;
    @Column(name = "workflow_name", nullable = false) private String name;
    private String description;
    @Column(nullable = false) private int version;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition_json", nullable = false, columnDefinition = "jsonb") private String definitionJson;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected WorkflowDefinitionEntity() { }
    public WorkflowDefinitionEntity(UUID id, String name, String description, int version, String definitionJson, Instant createdAt) {
        this.id = id; this.name = name; this.description = description; this.version = version;
        this.definitionJson = definitionJson; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getVersion() { return version; }
    public String getDefinitionJson() { return definitionJson; }
    public Instant getCreatedAt() { return createdAt; }
}
