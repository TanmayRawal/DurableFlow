package io.tanmayrawal.durableflow.worker;

import io.tanmayrawal.durableflow.run.WorkflowRunService.TaskResponse;
import org.springframework.stereotype.Component;

@Component
class NoopTaskHandler implements TaskHandler {
    @Override public boolean supports(String handlerType) { return "noop".equals(handlerType); }
    @Override public void execute(TaskResponse task) { /* Deterministic smoke-test handler for end-to-end workflow demos. */ }
}
