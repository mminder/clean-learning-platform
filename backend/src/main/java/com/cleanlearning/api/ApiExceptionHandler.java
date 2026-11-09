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
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * Maps exceptions to {@code ProblemDetail} (RFC 9457) bodies, see the errors in _docs/architecture/backend.md. Spring
 * MVC's own exceptions are handled by {@link ResponseEntityExceptionHandler}: an unknown route answers 404, and a
 * malformed request, such as an id that isn't a UUID, answers 400. A Bean Validation failure answers 400 with every
 * invalid field in {@code errors}. A business rule that says no answers 409 with its {@code code}. The status of a
 * returned {@code ProblemDetail} is the response's status.
 * <p>
 * {@code openapi.json} doesn't list these answers on every route: each controller method declares the ones it can
 * give, with {@link ProblemResponses}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler
    ProblemDetail handleNotFound(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /**
     * In this code base, {@link IllegalArgumentException} means an invalid value: a value object rejected a value
     * that Bean Validation let through, such as a name of only no-break spaces.
     */
    @ExceptionHandler
    ProblemDetail handleInvalidValue(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /** The message is written for the user, and the frontend shows it; {@code code} is stable (D-34). */
    @ExceptionHandler
    ProblemDetail handleBusinessRule(BusinessRuleException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setProperty("code", exception.code());
        return problem;
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

    /**
     * An upload over the multipart limit of {@code application.yml}, 2 MB for an image (D-21), answers 400 like an
     * image of another type, not Spring's 413. The servlet container stops reading at the limit, so the use case never
     * sees the file.
     */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "An image is at most 2 MB.");
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** An entry of a 400's {@code errors}: the field's path as Spring reports it, like {@code packages[1].price}. */
    record InvalidField(String field, String message) {
    }
}
