package com.cleanlearning.api;

import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.cleanlearning.domain.shared.NotFoundException;

/**
 * Maps exceptions to {@code ProblemDetail} (RFC 9457) bodies, see the errors in _docs/architecture/backend.md. Spring
 * MVC's own exceptions are handled by {@link ResponseEntityExceptionHandler}: an unknown route answers 404, and a
 * malformed request, such as an id that isn't a UUID, answers 400. A Bean Validation failure answers 400 with every
 * invalid field in {@code errors}.
 * <p>
 * The {@code @ResponseStatus} annotations make springdoc list these answers with the {@code ProblemDetail} schema in
 * {@code openapi.json} ({@code OpenApiConfig}).
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * In this code base, {@link IllegalArgumentException} means an invalid value: a value object rejected a value
     * that Bean Validation let through, such as a name of only no-break spaces.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail handleInvalidValue(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** Spring's 400 for an invalid request body, plus {@code errors}: each invalid field, sorted by its path. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(InvalidField::field).thenComparing(InvalidField::message))
                .toList();
        exception.getBody().setProperty("errors", errors);
        return super.handleMethodArgumentNotValid(exception, headers, status, request);
    }

    /** An entry of a 400's {@code errors}: the field's path as Spring reports it, like {@code packages[1].price}. */
    record InvalidField(String field, String message) {
    }
}
