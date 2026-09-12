package io.tanmayrawal.durableflow.dispatch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisOutboxPublisher {
    public static final String TASK_STREAM = "durableflow.tasks";
    private final OutboxEventRepository repository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    RedisOutboxPublisher(OutboxEventRepository repository, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.repository = repository; this.redis = redis; this.objectMapper = objectMapper;
    }
    @Scheduled(fixedDelayString = "${durableflow.outbox-delay-ms:1000}", initialDelayString = "${durableflow.outbox-initial-delay-ms:0}")
    @Transactional
    void publishPendingEvents() {
        for (OutboxEventEntity event : repository.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                Map<String, String> message = new LinkedHashMap<>(objectMapper.readValue(event.getPayloadJson(), new TypeReference<Map<String, String>>() { }));
                message.put("eventId", event.getId().toString()); message.put("eventType", event.getEventType());
                redis.opsForStream().add(StreamRecords.mapBacked(message).withStreamKey(TASK_STREAM));
                event.markPublished(Instant.now());
            } catch (Exception exception) {
                event.recordPublishAttempt();
                throw new IllegalStateException("Could not publish outbox event " + event.getId(), exception);
            }
        }
    }
}
