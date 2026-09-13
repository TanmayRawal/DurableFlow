package io.tanmayrawal.durableflow.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class WorkflowRunControllerTest {
    private final WorkflowRunService service = mock(WorkflowRunService.class);

    @Test
    void failsClosedWhenTheOperatorTokenIsNotConfigured() {
        WorkflowRunController controller = new WorkflowRunController(service, "", "trusted-console");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.claim(UUID.randomUUID(), UUID.randomUUID(), "any-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsAnIncorrectOperatorToken() {
        WorkflowRunController controller = new WorkflowRunController(service, "correct-token", "trusted-console");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.complete(UUID.randomUUID(), UUID.randomUUID(), "wrong-token"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void usesTheServerConfiguredWorkerIdentityAfterAuthorization() {
        WorkflowRunController controller = new WorkflowRunController(service, "correct-token", "trusted-console");
        UUID runId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();

        assertDoesNotThrow(() -> controller.claim(runId, taskId, "correct-token"));

        verify(service).claim(runId, taskId, "trusted-console");
    }
}
