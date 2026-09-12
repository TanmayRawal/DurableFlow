package io.tanmayrawal.durableflow.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tanmayrawal.durableflow.workflow.WorkflowDefinitionService;
import io.tanmayrawal.durableflow.workflow.WorkflowGraph;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "durableflow.outbox-initial-delay-ms=600000",
        "durableflow.recovery-initial-delay-ms=600000"
})
@Testcontainers
class WorkflowRunIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    @Autowired WorkflowDefinitionService definitions;
    @Autowired WorkflowRunService runs;
    @Autowired ObjectMapper objectMapper;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test void schedulesDependentTasksOnlyAfterTheirPredecessorCompletes() {
        WorkflowGraph graph = new WorkflowGraph(
                List.of(new WorkflowGraph.WorkflowNode("validate", "noop"), new WorkflowGraph.WorkflowNode("notify", "noop")),
                List.of(new WorkflowGraph.WorkflowEdge("validate", "notify")));
        var definition = definitions.create(new WorkflowDefinitionService.CreateWorkflowRequest("integration-test", "test graph", graph));
        var run = runs.start(definition.id(), "test-key-1", objectMapper.createObjectNode());
        var root = run.tasks().stream().filter(task -> task.nodeKey().equals("validate")).findFirst().orElseThrow();
        assertEquals(TaskStatus.READY, root.status());
        runs.claim(run.id(), root.id(), "worker-test");
        var afterCompletion = runs.complete(run.id(), root.id(), "worker-test");
        assertTrue(afterCompletion.tasks().stream().anyMatch(task -> task.nodeKey().equals("notify") && task.status() == TaskStatus.READY));
    }
}
