package io.tanmayrawal.durableflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class WorkflowDefinitionServiceTest {
    @Test
    void incrementsTheVersionFromTheLatestDefinitionWithTheSameName() {
        WorkflowDefinitionRepository repository = Mockito.mock(WorkflowDefinitionRepository.class);
        WorkflowDefinitionEntity current = new WorkflowDefinitionEntity(UUID.randomUUID(), "invoice-delivery", "old", 4, "{}", Instant.now());
        when(repository.findTopByNameOrderByVersionDesc("invoice-delivery")).thenReturn(Optional.of(current));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        WorkflowDefinitionService service = new WorkflowDefinitionService(repository, new WorkflowGraphValidator(), new ObjectMapper());
        WorkflowGraph graph = new WorkflowGraph(List.of(new WorkflowGraph.WorkflowNode("deliver", "noop", null)), List.of());

        var created = service.create(new WorkflowDefinitionService.CreateWorkflowRequest("invoice-delivery", "new", graph));

        assertEquals(5, created.version());
        ArgumentCaptor<WorkflowDefinitionEntity> saved = ArgumentCaptor.forClass(WorkflowDefinitionEntity.class);
        Mockito.verify(repository).save(saved.capture());
        assertEquals(5, saved.getValue().getVersion());
    }
}
