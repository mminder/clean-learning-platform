package com.cleanlearning.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Only an {@link InvalidValueException} is the caller's fault; any other exception is a bug. */
class ApiExceptionHandlerTest {

  private final MockMvcTester mockMvc =
      MockMvcTester.of(
          List.of(new Thrower()),
          builder -> builder.setControllerAdvice(new ApiExceptionHandler()).build());

  @Test
  void answers400WithTheMessageForAnInvalidValue() {
    assertThat(mockMvc.get().uri("/invalid-value"))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("A name is not blank");
  }

  @Test
  void doesNotAnswer400ForAnIllegalArgumentException() {
    assertThat(mockMvc.get().uri("/illegal-argument"))
        .failure()
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @RestController
  static class Thrower {

    @GetMapping("/invalid-value")
    String invalidValue() {
      throw new InvalidValueException("A name is not blank");
    }

    @GetMapping("/illegal-argument")
    String illegalArgument() {
      throw new IllegalArgumentException("internal detail");
    }
  }
}
