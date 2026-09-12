package io.tanmayrawal.durableflow.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record WorkflowGraph(
        @NotEmpty List<@Valid WorkflowNode> nodes,
        List<@Valid WorkflowEdge> edges) {
    public record WorkflowNode(@NotBlank String key, @NotBlank String handlerType) { }
    public record WorkflowEdge(@NotBlank String from, @NotBlank String to) { }
}
