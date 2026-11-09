package com.cleanlearning.api.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.user.response.ExpertResponse;
import com.cleanlearning.application.user.ListExpertsUseCase;

/**
 * The experts an admin can choose from for a consultation offering (UC-16). Security allows admins only.
 */
@RestController
class ExpertController {

    private final ListExpertsUseCase listExperts;

    ExpertController(ListExpertsUseCase listExperts) {
        this.listExperts = listExperts;
    }

    @GetMapping("/api/admin/experts")
    List<ExpertResponse> listExperts() {
        return listExperts.invoke().stream().map(ExpertResponse::from).toList();
    }
}
