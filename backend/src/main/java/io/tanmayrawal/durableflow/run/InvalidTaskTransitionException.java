package io.tanmayrawal.durableflow.run;

public class InvalidTaskTransitionException extends RuntimeException {
    public InvalidTaskTransitionException(String message) { super(message); }
}
