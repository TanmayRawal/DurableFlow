package io.tanmayrawal.durableflow.run;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class WorkflowRunController {
    private final WorkflowRunService service;
    private final String operatorToken;
    private final String operatorWorkerId;

    WorkflowRunController(WorkflowRunService service,
                          @Value("${durableflow.operator.token:}") String operatorToken,
                          @Value("${durableflow.operator.worker-id:console-operator}") String operatorWorkerId) {
        this.service = service;
        this.operatorToken = operatorToken;
        this.operatorWorkerId = operatorWorkerId;
    }

    @PostMapping("/workflows/{workflowDefinitionId}/runs")
    @ResponseStatus(HttpStatus.CREATED)
    WorkflowRunService.WorkflowRunResponse start(@PathVariable UUID workflowDefinitionId, @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody(required = false) JsonNode input) {
        return service.start(workflowDefinitionId, idempotencyKey, input);
    }

    @GetMapping("/runs/{runId}")
    WorkflowRunService.WorkflowRunResponse get(@PathVariable UUID runId) { return service.get(runId); }

    @PostMapping("/runs/{runId}/tasks/{taskId}/claim")
    WorkflowRunService.WorkflowRunResponse claim(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader(value = "X-Operator-Token", required = false) String token) {
        authorize(token); return service.claim(runId, taskId, operatorWorkerId);
    }

    @PostMapping("/runs/{runId}/tasks/{taskId}/complete")
    WorkflowRunService.WorkflowRunResponse complete(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader(value = "X-Operator-Token", required = false) String token) {
        authorize(token); return service.complete(runId, taskId, operatorWorkerId);
    }

    @PostMapping("/runs/{runId}/tasks/{taskId}/heartbeat")
    WorkflowRunService.WorkflowRunResponse heartbeat(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader(value = "X-Operator-Token", required = false) String token) {
        authorize(token); return service.heartbeat(runId, taskId, operatorWorkerId);
    }

    @PostMapping("/runs/{runId}/tasks/{taskId}/fail")
    WorkflowRunService.WorkflowRunResponse fail(@PathVariable UUID runId, @PathVariable UUID taskId, @RequestHeader(value = "X-Operator-Token", required = false) String token, @RequestBody FailureRequest request) {
        authorize(token); return service.fail(runId, taskId, operatorWorkerId, request.reason());
    }

    private void authorize(String suppliedToken) {
        if (suppliedToken == null || operatorToken.isBlank() || !MessageDigest.isEqual(operatorToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Operator token is required for manual task controls");
        }
    }

    public record FailureRequest(String reason) { }
}
