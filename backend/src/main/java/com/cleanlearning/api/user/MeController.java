package com.cleanlearning.api.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.user.response.MeResponse;
import com.cleanlearning.application.user.GetCurrentUserUseCase;
import com.cleanlearning.domain.user.CurrentUser;

/**
 * The logged-in user (UC-01). Without a login, security answers 401 before this controller.
 */
@RestController
class MeController {

    private final GetCurrentUserUseCase getCurrentUser;

    MeController(GetCurrentUserUseCase getCurrentUser) {
        this.getCurrentUser = getCurrentUser;
    }

    @GetMapping("/api/me")
    MeResponse getMe(@AuthenticationPrincipal CurrentUser currentUser) {
        return MeResponse.from(getCurrentUser.invoke(currentUser));
    }
}
