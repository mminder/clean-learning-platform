package com.cleanlearning.api.user;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.user.request.PocLoginRequest;
import com.cleanlearning.api.user.response.MeResponse;
import com.cleanlearning.api.user.response.PocUserResponse;
import com.cleanlearning.application.user.ListPocUsersUseCase;
import com.cleanlearning.application.user.LogInPocUserUseCase;
import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.UserId;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * UC-01's PoC login: pick a seeded user. It exists only in the {@code poc} profile (D-25), so elsewhere its routes
 * answer 404.
 */
@RestController
@RequestMapping("/api/poc")
@Profile("poc")
class PocLoginController {

    private final ListPocUsersUseCase listPocUsers;

    private final LogInPocUserUseCase logInPocUser;

    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    private final SecurityContextRepository securityContextRepository;

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    PocLoginController(ListPocUsersUseCase listPocUsers, LogInPocUserUseCase logInPocUser,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository) {
        this.listPocUsers = listPocUsers;
        this.logInPocUser = logInPocUser;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/users")
    List<PocUserResponse> listUsers() {
        return listPocUsers.invoke().stream().map(PocUserResponse::from).toList();
    }

    /**
     * Starts the session like Spring Security's own logins do: a new session id and a new CSRF token (see
     * {@code SecurityConfig}), then the authentication in the session.
     */
    @PostMapping("/login")
    MeResponse logIn(@Valid @RequestBody PocLoginRequest login, HttpServletRequest request,
            HttpServletResponse response) {
        User user = logInPocUser.invoke(new UserId(login.userId()));

        Authentication authentication = CurrentUserAuthentication.of(user.toCurrentUser());
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return MeResponse.from(user);
    }
}
