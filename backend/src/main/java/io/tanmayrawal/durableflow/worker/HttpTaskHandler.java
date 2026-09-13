package io.tanmayrawal.durableflow.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tanmayrawal.durableflow.run.WorkflowRunService.TaskResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** Executes a configured HTTPS webhook. Non-2xx responses are retried by DurableFlow. */
@Component
class HttpTaskHandler implements TaskHandler {
    private final ObjectMapper mapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    HttpTaskHandler(ObjectMapper mapper) { this.mapper = mapper; }
    @Override public boolean supports(String handlerType) { return "http".equals(handlerType); }
    @Override public void execute(TaskResponse task) throws Exception {
        JsonNode config = mapper.readTree(task.handlerConfigJson());
        String url = config.path("url").asText();
        if (!url.startsWith("https://")) throw new IllegalArgumentException("HTTP tasks require an https:// URL");
        String method = config.path("method").asText("POST").toUpperCase();
        String body = config.path("body").isMissingNode() ? "{}" : mapper.writeValueAsString(config.path("body"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("Webhook returned HTTP " + response.statusCode());
    }
}
