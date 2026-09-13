package io.tanmayrawal.durableflow.worker;

import io.tanmayrawal.durableflow.run.TaskExecutionEntity;
import io.tanmayrawal.durableflow.run.TaskExecutionRepository;
import io.tanmayrawal.durableflow.run.TaskStatus;
import io.tanmayrawal.durableflow.run.WorkflowRunService;
import io.tanmayrawal.durableflow.run.WorkflowRunService.TaskResponse;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Executes ready durable tasks from PostgreSQL as the recovery-safe worker source. */
@Component
@ConditionalOnProperty(name = "durableflow.worker.enabled", havingValue = "true")
class RedisTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(RedisTaskWorker.class);
    private final TaskExecutionRepository tasks;
    private final WorkflowRunService runs;
    private final List<TaskHandler> handlers;
    private final String workerId;

    RedisTaskWorker(TaskExecutionRepository tasks, WorkflowRunService runs, List<TaskHandler> handlers,
                    @org.springframework.beans.factory.annotation.Value("${durableflow.worker.id}") String workerId) {
        this.tasks = tasks; this.runs = runs; this.handlers = handlers; this.workerId = workerId;
        log.info("DurableFlow worker enabled as {} with {} handler(s)", workerId, handlers.size());
    }

    @Scheduled(fixedDelayString = "${durableflow.worker-poll-delay-ms:500}")
    void poll() {
        tasks.findTop20ByStatusOrderByCreatedAtAsc(TaskStatus.READY).forEach(this::process);
    }

    private void process(TaskExecutionEntity pending) {
        try {
            var claimed = runs.claim(pending.getWorkflowRunId(), pending.getId(), workerId);
            TaskResponse task = claimed.tasks().stream().filter(candidate -> candidate.id().equals(pending.getId())).findFirst().orElseThrow();
            TaskHandler handler = handlers.stream().filter(candidate -> candidate.supports(task.handlerType())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No worker handler is registered for " + task.handlerType()));
            handler.execute(task);
            runs.complete(pending.getWorkflowRunId(), pending.getId(), workerId);
        } catch (Exception exception) {
            log.warn("Task {} could not be processed; the lease recovery flow will retry it", pending.getId(), exception);
        }
    }
}
