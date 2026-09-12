package io.tanmayrawal.durableflow.workflow;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowDefinitionController {
    private final WorkflowDefinitionService service;
    WorkflowDefinitionController(WorkflowDefinitionService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WorkflowDefinitionService.WorkflowDefinitionResponse create(@Valid @RequestBody WorkflowDefinitionService.CreateWorkflowRequest request) { return service.create(request); }
    @GetMapping("/{id}")
    WorkflowDefinitionService.WorkflowDefinitionResponse get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping("/validate")
    Map<String, Boolean> validate(@Valid @RequestBody WorkflowGraph graph) { service.validate(graph); return Map.of("valid", true); }
}
