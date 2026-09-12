package io.tanmayrawal.durableflow.realtime;

import io.tanmayrawal.durableflow.run.WorkflowRunService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class WebSocketRunBroadcaster {
    private final SimpMessagingTemplate messages;
    private final WorkflowRunService runs;
    WebSocketRunBroadcaster(SimpMessagingTemplate messages, WorkflowRunService runs) { this.messages = messages; this.runs = runs; }
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishCommittedState(WorkflowRunStateChanged event) { messages.convertAndSend("/topic/runs/" + event.runId(), runs.get(event.runId())); }
}
