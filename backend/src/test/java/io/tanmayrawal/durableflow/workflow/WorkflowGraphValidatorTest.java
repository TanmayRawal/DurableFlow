package io.tanmayrawal.durableflow.workflow;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkflowGraphValidatorTest {
    private final WorkflowGraphValidator validator = new WorkflowGraphValidator();
    @Test void acceptsAnAcyclicGraph() {
        var graph = new WorkflowGraph(List.of(new WorkflowGraph.WorkflowNode("a", "http", null), new WorkflowGraph.WorkflowNode("b", "email", null)), List.of(new WorkflowGraph.WorkflowEdge("a", "b")));
        assertDoesNotThrow(() -> validator.validate(graph));
    }
    @Test void rejectsCycles() {
        var graph = new WorkflowGraph(List.of(new WorkflowGraph.WorkflowNode("a", "http", null), new WorkflowGraph.WorkflowNode("b", "email", null)), List.of(new WorkflowGraph.WorkflowEdge("a", "b"), new WorkflowGraph.WorkflowEdge("b", "a")));
        assertThrows(InvalidWorkflowException.class, () -> validator.validate(graph));
    }
}
