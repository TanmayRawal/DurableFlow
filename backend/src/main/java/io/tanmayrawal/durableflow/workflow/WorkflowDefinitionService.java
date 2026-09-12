package io.tanmayrawal.durableflow.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowDefinitionService {
    private final WorkflowDefinitionRepository repository;
    private final WorkflowGraphValidator validator;
    private final ObjectMapper objectMapper;
    WorkflowDefinitionService(WorkflowDefinitionRepository repository, WorkflowGraphValidator validator, ObjectMapper objectMapper) {
        this.repository = repository; this.validator = validator; this.objectMapper = objectMapper;
    }
    public WorkflowDefinitionResponse create(CreateWorkflowRequest request) {
        validator.validate(request.graph());
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity(UUID.randomUUID(), request.name(), request.description(), 1, serialize(request.graph()), Instant.now());
        return toResponse(repository.save(entity));
    }
    public WorkflowDefinitionResponse get(UUID id) {
        return repository.findById(id).map(this::toResponse).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow not found"));
    }
    public void validate(WorkflowGraph graph) { validator.validate(graph); }
    private String serialize(WorkflowGraph graph) {
        try { return objectMapper.writeValueAsString(graph); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Could not serialize workflow graph", exception); }
    }
    private WorkflowDefinitionResponse toResponse(WorkflowDefinitionEntity entity) {
        try { return new WorkflowDefinitionResponse(entity.getId(), entity.getName(), entity.getDescription(), entity.getVersion(), objectMapper.readValue(entity.getDefinitionJson(), WorkflowGraph.class), entity.getCreatedAt()); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Stored workflow definition is invalid", exception); }
    }
    public record CreateWorkflowRequest(@NotBlank String name, String description, @Valid WorkflowGraph graph) { }
    public record WorkflowDefinitionResponse(UUID id, String name, String description, int version, WorkflowGraph graph, Instant createdAt) { }
}
