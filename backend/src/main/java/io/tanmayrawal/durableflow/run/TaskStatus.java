package io.tanmayrawal.durableflow.run;

public enum TaskStatus {
    PENDING, READY, RUNNING, RETRYING, SUCCEEDED, FAILED, DEAD_LETTER, CANCELLED
}
