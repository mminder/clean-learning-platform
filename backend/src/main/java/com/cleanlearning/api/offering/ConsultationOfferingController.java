package com.cleanlearning.api.offering;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.offering.request.ConsultationOfferingRequest;
import com.cleanlearning.api.offering.request.ConsultationPackageRequest;
import com.cleanlearning.api.offering.response.ConsultationOfferingResponse;
import com.cleanlearning.api.offering.response.ConsultationOfferingSummaryResponse;
import com.cleanlearning.application.offering.ConsultationOfferingCommand;
import com.cleanlearning.application.offering.CreateConsultationOfferingUseCase;
import com.cleanlearning.application.offering.EditConsultationOfferingUseCase;
import com.cleanlearning.application.offering.GetConsultationOfferingUseCase;
import com.cleanlearning.application.offering.ListConsultationOfferingsUseCase;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.ConsultationPackageEntry;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;

import jakarta.validation.Valid;

/**
 * The admin's consultation offerings (UC-16).
 */
@RestController
@RequestMapping(ConsultationOfferingController.PATH)
class ConsultationOfferingController {

    static final String PATH = "/api/admin/consultation-offerings";

    private final ListConsultationOfferingsUseCase listConsultationOfferings;

    private final GetConsultationOfferingUseCase getConsultationOffering;

    private final CreateConsultationOfferingUseCase createConsultationOffering;

    private final EditConsultationOfferingUseCase editConsultationOffering;

    ConsultationOfferingController(ListConsultationOfferingsUseCase listConsultationOfferings,
            GetConsultationOfferingUseCase getConsultationOffering,
            CreateConsultationOfferingUseCase createConsultationOffering,
            EditConsultationOfferingUseCase editConsultationOffering) {
        this.listConsultationOfferings = listConsultationOfferings;
        this.getConsultationOffering = getConsultationOffering;
        this.createConsultationOffering = createConsultationOffering;
        this.editConsultationOffering = editConsultationOffering;
    }

    @GetMapping
    List<ConsultationOfferingSummaryResponse> listConsultationOfferings() {
        return listConsultationOfferings.invoke().stream().map(ConsultationOfferingSummaryResponse::from).toList();
    }

    /** {@code @ResponseStatus} tells springdoc the status that {@code ResponseEntity.created} sets. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<ConsultationOfferingResponse> createConsultationOffering(
            @Valid @RequestBody ConsultationOfferingRequest request) {
        ConsultationOffering offering = createConsultationOffering.invoke(toCommand(request));
        return ResponseEntity.created(URI.create(PATH + "/" + offering.id().value()))
                .body(ConsultationOfferingResponse.from(offering));
    }

    @GetMapping("/{id}")
    ConsultationOfferingResponse getConsultationOffering(@PathVariable UUID id) {
        return ConsultationOfferingResponse.from(getConsultationOffering.invoke(new OfferingId(id)));
    }

    @PutMapping("/{id}")
    ConsultationOfferingResponse editConsultationOffering(@PathVariable UUID id,
            @Valid @RequestBody ConsultationOfferingRequest request) {
        return ConsultationOfferingResponse.from(editConsultationOffering.invoke(new OfferingId(id),
                toCommand(request)));
    }

    /** A value object that rejects a value Bean Validation let through throws, and the API answers 400. */
    private static ConsultationOfferingCommand toCommand(ConsultationOfferingRequest request) {
        return new ConsultationOfferingCommand(new OfferingName(request.name()), request.description(),
                new SessionDuration(request.sessionDurationMinutes()),
                new CancellationDeadline(request.cancellationDeadlineHours()),
                request.packages().stream().map(ConsultationOfferingController::toEntry).toList());
    }

    private static ConsultationPackageEntry toEntry(ConsultationPackageRequest request) {
        SessionCount sessionCount = new SessionCount(request.sessionCount());
        Money price = Money.chf(request.price());
        return request.id() == null ? ConsultationPackageEntry.added(sessionCount, price)
                : ConsultationPackageEntry.kept(new ConsultationPackageId(request.id()), sessionCount, price);
    }
}
