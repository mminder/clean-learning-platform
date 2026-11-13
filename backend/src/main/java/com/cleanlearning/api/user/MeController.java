package com.cleanlearning.api.user;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.user.response.MeResponse;
import com.cleanlearning.application.user.GetCurrentUserUseCase;
import com.cleanlearning.domain.user.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in user (UC-01). Without a login, {@code @PreAuthorize} stops the call and security
 * answers 401.
 */
@RestController
@PreAuthorize("isAuthenticated()")
class MeController {

  private final GetCurrentUserUseCase getCurrentUser;

  MeController(GetCurrentUserUseCase getCurrentUser) {
    this.getCurrentUser = getCurrentUser;
  }

  /** 404 if the session's user no longer exists. */
  @GetMapping("/api/me")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status404
  MeResponse getMe(@AuthenticationPrincipal CurrentUser currentUser) {
    return MeResponse.from(getCurrentUser.invoke(currentUser));
  }
}
