package io.tanmayrawal.durableflow.run;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public integration entry point for self-hosted automations. */
@RestController
@RequestMapping("/api/hooks/workflows")
class WebhookTriggerController {
    private final WorkflowRunService runs;
    WebhookTriggerController(WorkflowRunService runs) { this.runs = runs; }

    @PostMapping("/{workflowDefinitionId}")
    @ResponseStatus(HttpStatus.CREATED)
    WorkflowRunService.WorkflowRunResponse trigger(
            @PathVariable UUID workflowDefinitionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) JsonNode payload) {
        String key = idempotencyKey == null || idempotencyKey.isBlank() ? UUID.randomUUID().toString() : idempotencyKey;
        return runs.start(workflowDefinitionId, key, payload);
    }
}
