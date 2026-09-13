package io.tanmayrawal.durableflow.run;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tanmayrawal.durableflow.workflow.WorkflowDefinitionEntity;
import io.tanmayrawal.durableflow.workflow.WorkflowGraph;
import io.tanmayrawal.durableflow.dispatch.OutboxService;
import io.tanmayrawal.durableflow.realtime.WorkflowRunStateChanged;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowRunService {
    private final WorkflowRunRepository runRepository;
    private final TaskExecutionRepository taskRepository;
    private final io.tanmayrawal.durableflow.workflow.WorkflowDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;
    private final ApplicationEventPublisher events;

    WorkflowRunService(WorkflowRunRepository runRepository, TaskExecutionRepository taskRepository,
                       io.tanmayrawal.durableflow.workflow.WorkflowDefinitionRepository definitionRepository, ObjectMapper objectMapper, OutboxService outboxService, ApplicationEventPublisher events) {
        this.runRepository = runRepository; this.taskRepository = taskRepository;
        this.definitionRepository = definitionRepository; this.objectMapper = objectMapper; this.outboxService = outboxService; this.events = events;
    }

    @Transactional
    public WorkflowRunResponse start(UUID workflowDefinitionId, String idempotencyKey, JsonNode input) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        var existing = runRepository.findByWorkflowDefinitionIdAndIdempotencyKey(workflowDefinitionId, idempotencyKey);
        if (existing.isPresent()) return response(existing.get(), taskRepository.findByWorkflowRunId(existing.get().getId()));
        WorkflowDefinitionEntity definition = definitionRepository.findById(workflowDefinitionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow definition not found"));
        WorkflowGraph graph = deserializeGraph(definition.getDefinitionJson());
        Instant now = Instant.now();
        WorkflowRunEntity run = runRepository.save(new WorkflowRunEntity(UUID.randomUUID(), workflowDefinitionId, serializeInput(input), idempotencyKey, now));
        Set<String> nodesWithInboundEdges = graph.edges() == null ? Set.of() : graph.edges().stream().map(WorkflowGraph.WorkflowEdge::to).collect(Collectors.toSet());
        List<TaskExecutionEntity> tasks = graph.nodes().stream()
                .map(node -> new TaskExecutionEntity(UUID.randomUUID(), run.getId(), node.key(), node.handlerType(),
                        serializeInput(node.config()),
                        nodesWithInboundEdges.contains(node.key()) ? TaskStatus.PENDING : TaskStatus.READY, now))
                .toList();
        taskRepository.saveAll(tasks);
        tasks.stream().filter(task -> task.getStatus() == TaskStatus.READY).forEach(outboxService::taskReady);
        changed(run.getId());
        return response(run, tasks);
    }

    @Transactional
    public WorkflowRunResponse claim(UUID runId, UUID taskId, String workerId) {
        WorkflowRunEntity run = findRun(runId);
        TaskExecutionEntity task = findTask(runId, taskId);
        task.claim(workerId, Instant.now(), java.time.Duration.ofSeconds(30));
        changed(runId);
        return response(run, taskRepository.findByWorkflowRunId(runId));
    }

    @Transactional
    public WorkflowRunResponse complete(UUID runId, UUID taskId, String workerId) {
        WorkflowRunEntity run = findRun(runId);
        TaskExecutionEntity completedTask = findTask(runId, taskId);
        completedTask.complete(workerId, Instant.now());
        WorkflowGraph graph = graphFor(run);
        List<TaskExecutionEntity> tasks = taskRepository.findByWorkflowRunId(runId);
        Map<String, TaskExecutionEntity> byNode = tasks.stream().collect(Collectors.toMap(TaskExecutionEntity::getNodeKey, Function.identity()));
        for (WorkflowGraph.WorkflowEdge edge : safeEdges(graph)) {
            if (edge.from().equals(completedTask.getNodeKey())) {
                TaskExecutionEntity dependent = byNode.get(edge.to());
                if (dependent.getStatus() == TaskStatus.PENDING && predecessorsSucceeded(edge.to(), graph, byNode)) {
                    dependent.makeReady(Instant.now());
                    outboxService.taskReady(dependent);
                }
            }
        }
        if (tasks.stream().allMatch(task -> task.getStatus() == TaskStatus.SUCCEEDED)) run.succeed(Instant.now());
        changed(runId);
        return response(run, tasks);
    }

    @Transactional
    public WorkflowRunResponse get(UUID runId) { WorkflowRunEntity run = findRun(runId); return response(run, taskRepository.findByWorkflowRunId(runId)); }

    @Transactional
    public WorkflowRunResponse heartbeat(UUID runId, UUID taskId, String workerId) {
        WorkflowRunEntity run = findRun(runId); TaskExecutionEntity task = findTask(runId, taskId);
        task.heartbeat(workerId, Instant.now(), java.time.Duration.ofSeconds(30));
        changed(runId);
        return response(run, taskRepository.findByWorkflowRunId(runId));
    }

    @Transactional
    public WorkflowRunResponse fail(UUID runId, UUID taskId, String workerId, String reason) {
        WorkflowRunEntity run = findRun(runId); TaskExecutionEntity task = findTask(runId, taskId);
        if (!workerId.equals(task.getLeaseOwner())) throw new InvalidTaskTransitionException("Task lease is owned by a different worker");
        task.fail(reason, Instant.now());
        changed(runId);
        return response(run, taskRepository.findByWorkflowRunId(runId));
    }

    @Transactional
    public RecoverySummary recoverAbandonedWork() {
        Instant now = Instant.now();
        List<TaskExecutionEntity> expired = taskRepository.findByStatusAndLeaseExpiresAtBefore(TaskStatus.RUNNING, now);
        expired.forEach(task -> task.fail("Worker lease expired", now));
        List<TaskExecutionEntity> retries = taskRepository.findByStatusAndNextAttemptAtLessThanEqual(TaskStatus.RETRYING, now);
        retries.forEach(task -> { task.makeReadyFromRetry(now); outboxService.taskReady(task); });
        expired.stream().map(TaskExecutionEntity::getWorkflowRunId).forEach(this::changed);
        retries.stream().map(TaskExecutionEntity::getWorkflowRunId).forEach(this::changed);
        return new RecoverySummary(expired.size(), retries.size());
    }

    private boolean predecessorsSucceeded(String nodeKey, WorkflowGraph graph, Map<String, TaskExecutionEntity> tasks) {
        return safeEdges(graph).stream().filter(edge -> edge.to().equals(nodeKey))
                .allMatch(edge -> tasks.get(edge.from()).getStatus() == TaskStatus.SUCCEEDED);
    }
    private void changed(UUID runId) { events.publishEvent(new WorkflowRunStateChanged(runId)); }
    private List<WorkflowGraph.WorkflowEdge> safeEdges(WorkflowGraph graph) { return graph.edges() == null ? List.of() : graph.edges(); }
    private WorkflowRunEntity findRun(UUID id) { return runRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow run not found")); }
    private TaskExecutionEntity findTask(UUID runId, UUID taskId) {
        TaskExecutionEntity task = taskRepository.findById(taskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        if (!task.getWorkflowRunId().equals(runId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task does not belong to workflow run");
        return task;
    }
    private WorkflowGraph graphFor(WorkflowRunEntity run) { return deserializeGraph(definitionRepository.findById(run.getWorkflowDefinitionId()).orElseThrow().getDefinitionJson()); }
    private WorkflowGraph deserializeGraph(String json) { try { return objectMapper.readValue(json, WorkflowGraph.class); } catch (JsonProcessingException e) { throw new IllegalStateException("Stored workflow graph is invalid", e); } }
    private String serializeInput(JsonNode input) { try { return objectMapper.writeValueAsString(input == null ? Map.of() : input); } catch (JsonProcessingException e) { throw new IllegalArgumentException("Input cannot be serialized", e); } }
    private WorkflowRunResponse response(WorkflowRunEntity run, Collection<TaskExecutionEntity> tasks) {
        return new WorkflowRunResponse(run.getId(), run.getWorkflowDefinitionId(), run.getStatus(), run.getCreatedAt(), run.getStartedAt(), run.getCompletedAt(),
                tasks.stream().map(t -> new TaskResponse(t.getId(), t.getNodeKey(), t.getHandlerType(), t.getHandlerConfigJson(), t.getStatus(), t.getAttempt())).toList());
    }
    public record WorkflowRunResponse(UUID id, UUID workflowDefinitionId, WorkflowRunStatus status, Instant createdAt, Instant startedAt, Instant completedAt, List<TaskResponse> tasks) { }
    public record TaskResponse(UUID id, String nodeKey, String handlerType, String handlerConfigJson, TaskStatus status, int attempt) { }
    public record RecoverySummary(int expiredLeasesRecovered, int retriesPromoted) { }
}
