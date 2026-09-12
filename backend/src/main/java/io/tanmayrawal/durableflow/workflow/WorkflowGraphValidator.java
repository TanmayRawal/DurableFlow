package io.tanmayrawal.durableflow.workflow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class WorkflowGraphValidator {
    public void validate(WorkflowGraph graph) {
        Set<String> nodeKeys = new HashSet<>();
        for (WorkflowGraph.WorkflowNode node : graph.nodes()) {
            if (!nodeKeys.add(node.key())) throw new InvalidWorkflowException("Duplicate node key: " + node.key());
        }
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (String key : nodeKeys) adjacency.put(key, new HashSet<>());
        List<WorkflowGraph.WorkflowEdge> edges = graph.edges() == null ? List.of() : graph.edges();
        for (WorkflowGraph.WorkflowEdge edge : edges) {
            if (!nodeKeys.contains(edge.from()) || !nodeKeys.contains(edge.to())) {
                throw new InvalidWorkflowException("Every edge must reference an existing node");
            }
            adjacency.get(edge.from()).add(edge.to());
        }
        Set<String> visiting = new HashSet<>(), visited = new HashSet<>();
        for (String key : nodeKeys) visit(key, adjacency, visiting, visited);
    }
    private void visit(String node, Map<String, Set<String>> graph, Set<String> visiting, Set<String> visited) {
        if (visited.contains(node)) return;
        if (!visiting.add(node)) throw new InvalidWorkflowException("Workflow graph contains a cycle at node: " + node);
        for (String next : graph.get(node)) visit(next, graph, visiting, visited);
        visiting.remove(node); visited.add(node);
    }
}
