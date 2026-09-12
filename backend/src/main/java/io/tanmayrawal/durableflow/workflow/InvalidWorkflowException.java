package io.tanmayrawal.durableflow.workflow;

public class InvalidWorkflowException extends RuntimeException {
    public InvalidWorkflowException(String message) { super(message); }
}
