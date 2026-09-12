package io.tanmayrawal.durableflow.run;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class TaskRecoveryScheduler {
    private final WorkflowRunService service;
    TaskRecoveryScheduler(WorkflowRunService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${durableflow.recovery-delay-ms:5000}", initialDelayString = "${durableflow.recovery-initial-delay-ms:0}")
    void recoverAbandonedWork() { service.recoverAbandonedWork(); }
}
