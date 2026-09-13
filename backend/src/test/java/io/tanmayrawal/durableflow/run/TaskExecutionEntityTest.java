package io.tanmayrawal.durableflow.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskExecutionEntityTest {
    @Test void enforcesTheTaskStateMachine() {
        Instant now = Instant.parse("2026-09-13T00:00:00Z");
        TaskExecutionEntity task = new TaskExecutionEntity(UUID.randomUUID(), UUID.randomUUID(), "validate", "http", "{}", TaskStatus.READY, now);
        assertThrows(InvalidTaskTransitionException.class, () -> task.complete("worker-a", now));
        task.claim("worker-a", now, java.time.Duration.ofSeconds(30));
        task.complete("worker-a", now);
        assertEquals(TaskStatus.SUCCEEDED, task.getStatus());
        assertEquals(1, task.getAttempt());
    }

    @Test void schedulesBackoffThenDeadLettersAfterTheRetryBudget() {
        Instant now = Instant.parse("2026-09-13T00:00:00Z");
        TaskExecutionEntity task = new TaskExecutionEntity(UUID.randomUUID(), UUID.randomUUID(), "validate", "http", "{}", TaskStatus.READY, now);
        for (int attempt = 1; attempt <= 3; attempt++) {
            task.claim("worker-a", now, java.time.Duration.ofSeconds(30));
            task.fail("transient failure", now);
            if (attempt < 3) task.makeReadyFromRetry(now.plusSeconds(300));
        }
        assertEquals(TaskStatus.DEAD_LETTER, task.getStatus());
    }
}
