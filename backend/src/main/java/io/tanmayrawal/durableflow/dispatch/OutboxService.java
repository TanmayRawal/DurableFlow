package io.tanmayrawal.durableflow.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tanmayrawal.durableflow.run.TaskExecutionEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OutboxService {
    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    OutboxService(OutboxEventRepository repository, ObjectMapper objectMapper) { this.repository = repository; this.objectMapper = objectMapper; }
    public void taskReady(TaskExecutionEntity task) {
        Map<String, String> payload = Map.of("runId", task.getWorkflowRunId().toString(), "taskId", task.getId().toString(), "handlerType", task.getHandlerType());
        try { repository.save(new OutboxEventEntity(UUID.randomUUID(), "TASK_READY", task.getId(), objectMapper.writeValueAsString(payload), Instant.now())); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Could not serialize task-ready event", exception); }
    }
}
