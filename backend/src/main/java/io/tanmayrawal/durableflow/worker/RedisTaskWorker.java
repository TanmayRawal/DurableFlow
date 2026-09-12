package io.tanmayrawal.durableflow.worker;

import io.tanmayrawal.durableflow.dispatch.RedisOutboxPublisher;
import io.tanmayrawal.durableflow.run.WorkflowRunService;
import io.tanmayrawal.durableflow.run.WorkflowRunService.TaskResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "durableflow.worker.enabled", havingValue = "true")
class RedisTaskWorker {
    private static final String GROUP = "durableflow-workers";
    private final StringRedisTemplate redis;
    private final WorkflowRunService runs;
    private final List<TaskHandler> handlers;
    private final String workerId;
    private volatile boolean groupCreated;

    RedisTaskWorker(StringRedisTemplate redis, WorkflowRunService runs, List<TaskHandler> handlers,
                    @org.springframework.beans.factory.annotation.Value("${durableflow.worker.id}") String workerId) {
        this.redis = redis; this.runs = runs; this.handlers = handlers; this.workerId = workerId;
    }

    @Scheduled(fixedDelayString = "${durableflow.worker-poll-delay-ms:500}")
    void poll() {
        if (!ensureConsumerGroup()) return;
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                Consumer.from(GROUP, workerId), StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
                StreamOffset.create(RedisOutboxPublisher.TASK_STREAM, ReadOffset.lastConsumed()));
        if (records == null) return;
        records.forEach(this::process);
    }

    private boolean ensureConsumerGroup() {
        if (groupCreated) return true;
        try {
            redis.opsForStream().createGroup(RedisOutboxPublisher.TASK_STREAM, ReadOffset.latest(), GROUP);
            groupCreated = true;
        } catch (DataAccessException exception) {
            String message = String.valueOf(exception.getMessage());
            if (message.contains("BUSYGROUP")) groupCreated = true;
            else return false; // The stream has not been created by an outbox event yet.
        }
        return groupCreated;
    }

    private void process(MapRecord<String, Object, Object> record) {
        try {
            UUID runId = UUID.fromString(String.valueOf(record.getValue().get("runId")));
            UUID taskId = UUID.fromString(String.valueOf(record.getValue().get("taskId")));
            WorkflowRunService.WorkflowRunResponse run = runs.claim(runId, taskId, workerId);
            TaskResponse task = run.tasks().stream().filter(candidate -> candidate.id().equals(taskId)).findFirst().orElseThrow();
            TaskHandler handler = handlers.stream().filter(candidate -> candidate.supports(task.handlerType())).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No worker handler is registered for " + task.handlerType()));
            handler.execute(task);
            runs.complete(runId, taskId, workerId);
            redis.opsForStream().acknowledge(RedisOutboxPublisher.TASK_STREAM, GROUP, record.getId());
        } catch (Exception exception) {
            // Do not acknowledge a failed message. Lease expiry/retry logic recovers the durable task state.
        }
    }
}
