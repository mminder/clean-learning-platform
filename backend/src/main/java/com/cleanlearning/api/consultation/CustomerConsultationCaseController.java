package com.cleanlearning.api.consultation;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.consultation.request.PurchaseConsultationPackageRequest;
import com.cleanlearning.api.consultation.response.CustomerConsultationCaseResponse;
import com.cleanlearning.application.consultation.GetCustomerConsultationCaseUseCase;
import com.cleanlearning.application.consultation.PurchaseConsultationPackageCommand;
import com.cleanlearning.application.consultation.PurchaseConsultationPackageUseCase;
import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.CurrentUser;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * The customer's cases (UC-03, UC-05). Security lets only users with {@code CUSTOMER} in; each use case is scoped to
 * the current user, so another customer's case answers 404.
 */
@RestController
@RequestMapping(CustomerConsultationCaseController.PATH)
class CustomerConsultationCaseController {

    static final String PATH = "/api/customer/consultation-cases";

    private final PurchaseConsultationPackageUseCase purchaseConsultationPackage;

    private final GetCustomerConsultationCaseUseCase getCustomerConsultationCase;

    CustomerConsultationCaseController(PurchaseConsultationPackageUseCase purchaseConsultationPackage,
            GetCustomerConsultationCaseUseCase getCustomerConsultationCase) {
        this.purchaseConsultationPackage = purchaseConsultationPackage;
        this.getCustomerConsultationCase = getCustomerConsultationCase;
    }

    /**
     * UC-03: buys a package; the current user is the case's customer. {@code @ResponseStatus} tells springdoc the
     * status that {@code ResponseEntity.created} sets, and {@code @ApiResponse} adds its {@code Location} header.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", headers = @Header(name = HttpHeaders.LOCATION,
            description = "The new case's URL, relative",
            schema = @Schema(type = "string", format = "uri-reference")))
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    @ProblemResponses.Status415
    ResponseEntity<CustomerConsultationCaseResponse> purchaseConsultationPackage(
            @AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody PurchaseConsultationPackageRequest request) {
        CustomerConsultationCase consultationCase = purchaseConsultationPackage.invoke(currentUser,
                toCommand(request));
        return ResponseEntity.created(URI.create(PATH + "/" + consultationCase.id().value()))
                .body(CustomerConsultationCaseResponse.from(consultationCase));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    CustomerConsultationCaseResponse getCustomerConsultationCase(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID id) {
        return CustomerConsultationCaseResponse.from(getCustomerConsultationCase.invoke(currentUser,
                new ConsultationCaseId(id)));
    }

    /** A value object that rejects a value Bean Validation let through throws, and the API answers 400. */
    private static PurchaseConsultationPackageCommand toCommand(PurchaseConsultationPackageRequest request) {
        return new PurchaseConsultationPackageCommand(new OfferingId(request.offeringId()),
                new ConsultationPackageId(request.packageId()), Money.chf(request.price()),
                new SessionCount(request.sessionCount()));
    }
}
