package com.cleanlearning.api;

import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.InvalidValueException;
import com.cleanlearning.domain.shared.NotFoundException;
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

/**
 * Maps exceptions to {@code ProblemDetail} (RFC 9457) bodies. {@code openapi.json} doesn't list
 * these answers on every route: each controller method declares its own, with {@link
 * ProblemResponses}.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler
  ProblemDetail handleNotFound(NotFoundException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  /**
   * Other exceptions, an {@link IllegalArgumentException} included, are server bugs and answer 500.
   */
  @ExceptionHandler
  ProblemDetail handleInvalidValue(InvalidValueException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler
  ProblemDetail handleBusinessRule(BusinessRuleException exception) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    problem.setProperty("code", exception.code());
    return problem;
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<InvalidField> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.comparing(InvalidField::field).thenComparing(InvalidField::message))
            .toList();
    exception.getBody().setProperty("errors", errors);
    return super.handleMethodArgumentNotValid(exception, headers, status, request);
  }

  /**
   * Answers 400 like an image of another type, not Spring's 413. The servlet container stops
   * reading at the limit, so the use case never sees the file.
   */
  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException exception,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "An image is at most 2 MB.");
    return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
  }

  record InvalidField(String field, String message) {}
}
