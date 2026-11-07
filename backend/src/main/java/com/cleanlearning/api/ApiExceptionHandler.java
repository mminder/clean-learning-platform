package com.cleanlearning.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.cleanlearning.domain.shared.NotFoundException;

/**
 * Maps exceptions to {@code ProblemDetail} (RFC 9457) bodies, see the errors in _docs/architecture/backend.md. Spring
 * MVC's own exceptions are handled by {@link ResponseEntityExceptionHandler}: an unknown route answers 404, and a
 * malformed request, such as an id that isn't a UUID, answers 400.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }
}
