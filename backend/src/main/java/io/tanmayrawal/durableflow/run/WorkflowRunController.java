package io.tanmayrawal.durableflow.run;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WorkflowRunController {
    private final WorkflowRunService service;
    WorkflowRunController(WorkflowRunService service) { this.service = service; }
    @PostMapping("/workflows/{workflowDefinitionId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    WorkflowRunService.WorkflowRunResponse start(@PathVariable UUID workflowDefinitionId, @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody(required = false) JsonNode input) { return service.start(workflowDefinitionId, idempotencyKey, input); }
    @GetMapping("/runs/{runId}")
    WorkflowRunService.WorkflowRunResponse get(@PathVariable UUID runId) { return service.get(runId); }
    @PostMapping("/runs/{runId}/tasks/{taskId}/claim")
    WorkflowRunService.WorkflowRunResponse claim(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader("Worker-Id") String workerId) { return service.claim(runId, taskId, workerId); }
    @PostMapping("/runs/{runId}/tasks/{taskId}/complete")
    WorkflowRunService.WorkflowRunResponse complete(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader("Worker-Id") String workerId) { return service.complete(runId, taskId, workerId); }
    @PostMapping("/runs/{runId}/tasks/{taskId}/heartbeat")
    WorkflowRunService.WorkflowRunResponse heartbeat(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader("Worker-Id") String workerId) { return service.heartbeat(runId, taskId, workerId); }
    @PostMapping("/runs/{runId}/tasks/{taskId}/fail")
    WorkflowRunService.WorkflowRunResponse fail(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader("Worker-Id") String workerId, @RequestBody FailureRequest request) { return service.fail(runId, taskId, workerId, request.reason()); }
    public record FailureRequest(String reason) { }
}
