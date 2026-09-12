package io.tanmayrawal.durableflow.workflow;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.tanmayrawal.durableflow.run.InvalidTaskTransitionException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(InvalidWorkflowException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    Map<String, String> invalidWorkflow(InvalidWorkflowException exception) { return Map.of("error", exception.getMessage()); }

    @ExceptionHandler(InvalidTaskTransitionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> invalidTaskTransition(InvalidTaskTransitionException exception) { return Map.of("error", exception.getMessage()); }
}
