package io.tanmayrawal.durableflow.worker;

import io.tanmayrawal.durableflow.run.WorkflowRunService.TaskResponse;

public interface TaskHandler {
    boolean supports(String handlerType);
    void execute(TaskResponse task) throws Exception;
}
