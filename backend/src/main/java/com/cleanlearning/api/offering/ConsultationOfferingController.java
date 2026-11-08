package com.cleanlearning.api.offering;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.offering.request.ConsultationOfferingRequest;
import com.cleanlearning.api.offering.request.ConsultationPackageRequest;
import com.cleanlearning.api.offering.response.ConsultationOfferingResponse;
import com.cleanlearning.api.offering.response.ConsultationOfferingSummaryResponse;
import com.cleanlearning.application.offering.ArchiveConsultationOfferingUseCase;
import com.cleanlearning.application.offering.ConsultationOfferingCommand;
import com.cleanlearning.application.offering.CreateConsultationOfferingUseCase;
import com.cleanlearning.application.offering.DeleteConsultationOfferingUseCase;
import com.cleanlearning.application.offering.EditConsultationOfferingUseCase;
import com.cleanlearning.application.offering.GetConsultationOfferingUseCase;
import com.cleanlearning.application.offering.ListConsultationOfferingsUseCase;
import com.cleanlearning.application.offering.PublishConsultationOfferingUseCase;
import com.cleanlearning.application.offering.RemoveConsultationOfferingImageUseCase;
import com.cleanlearning.application.offering.UnpublishConsultationOfferingUseCase;
import com.cleanlearning.application.offering.UploadConsultationOfferingImageUseCase;
import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.ConsultationPackageEntry;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.UserId;

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    private final PublishConsultationOfferingUseCase publishConsultationOffering;

    private final UnpublishConsultationOfferingUseCase unpublishConsultationOffering;

    private final ArchiveConsultationOfferingUseCase archiveConsultationOffering;

    private final DeleteConsultationOfferingUseCase deleteConsultationOffering;

    private final UploadConsultationOfferingImageUseCase uploadConsultationOfferingImage;

    private final RemoveConsultationOfferingImageUseCase removeConsultationOfferingImage;

    ConsultationOfferingController(ListConsultationOfferingsUseCase listConsultationOfferings,
            GetConsultationOfferingUseCase getConsultationOffering,
            CreateConsultationOfferingUseCase createConsultationOffering,
            EditConsultationOfferingUseCase editConsultationOffering,
            PublishConsultationOfferingUseCase publishConsultationOffering,
            UnpublishConsultationOfferingUseCase unpublishConsultationOffering,
            ArchiveConsultationOfferingUseCase archiveConsultationOffering,
            DeleteConsultationOfferingUseCase deleteConsultationOffering,
            UploadConsultationOfferingImageUseCase uploadConsultationOfferingImage,
            RemoveConsultationOfferingImageUseCase removeConsultationOfferingImage) {
        this.listConsultationOfferings = listConsultationOfferings;
        this.getConsultationOffering = getConsultationOffering;
        this.createConsultationOffering = createConsultationOffering;
        this.editConsultationOffering = editConsultationOffering;
        this.publishConsultationOffering = publishConsultationOffering;
        this.unpublishConsultationOffering = unpublishConsultationOffering;
        this.archiveConsultationOffering = archiveConsultationOffering;
        this.deleteConsultationOffering = deleteConsultationOffering;
        this.uploadConsultationOfferingImage = uploadConsultationOfferingImage;
        this.removeConsultationOfferingImage = removeConsultationOfferingImage;
    }

    @GetMapping
    List<ConsultationOfferingSummaryResponse> listConsultationOfferings() {
        return listConsultationOfferings.invoke().stream().map(ConsultationOfferingSummaryResponse::from).toList();
    }

    /**
     * {@code @ResponseStatus} tells springdoc the status that {@code ResponseEntity.created} sets, and
     * {@code @ApiResponse} adds its {@code Location} header to the contract.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @ApiResponse(responseCode = "201", headers = @Header(name = HttpHeaders.LOCATION,
            description = "The new offering's URL, relative",
            schema = @Schema(type = "string", format = "uri-reference")))
    @ProblemResponses.Status400
    ResponseEntity<ConsultationOfferingResponse> createConsultationOffering(
            @Valid @RequestBody ConsultationOfferingRequest request) {
        ConsultationOffering offering = createConsultationOffering.invoke(toCommand(request));
        return ResponseEntity.created(URI.create(PATH + "/" + offering.id().value()))
                .body(ConsultationOfferingResponse.from(offering));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    ConsultationOfferingResponse getConsultationOffering(@PathVariable UUID id) {
        return ConsultationOfferingResponse.from(getConsultationOffering.invoke(new OfferingId(id)));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    ConsultationOfferingResponse editConsultationOffering(@PathVariable UUID id,
            @Valid @RequestBody ConsultationOfferingRequest request) {
        return ConsultationOfferingResponse.from(editConsultationOffering.invoke(new OfferingId(id),
                toCommand(request)));
    }

    @PostMapping("/{id}/publish")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    ConsultationOfferingResponse publishConsultationOffering(@PathVariable UUID id) {
        return ConsultationOfferingResponse.from(publishConsultationOffering.invoke(new OfferingId(id)));
    }

    @PostMapping("/{id}/unpublish")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    ConsultationOfferingResponse unpublishConsultationOffering(@PathVariable UUID id) {
        return ConsultationOfferingResponse.from(unpublishConsultationOffering.invoke(new OfferingId(id)));
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    ConsultationOfferingResponse archiveConsultationOffering(@PathVariable UUID id) {
        return ConsultationOfferingResponse.from(archiveConsultationOffering.invoke(new OfferingId(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    void deleteConsultationOffering(@PathVariable UUID id) {
        deleteConsultationOffering.invoke(new OfferingId(id));
    }

    /**
     * The image, uploaded or replaced at once, apart from the form's save. The type comes from the file's first bytes,
     * not from its name or declared content type. A file over 2 MB stops at the multipart limit
     * ({@code application.yml}) and answers 400, like a file of another type.
     */
    @PutMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    @ProblemResponses.Status415
    ConsultationOfferingResponse uploadConsultationOfferingImage(@PathVariable UUID id,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ConsultationOfferingResponse.from(uploadConsultationOfferingImage.invoke(new OfferingId(id),
                file.getBytes()));
    }

    @DeleteMapping("/{id}/image")
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status404
    @ProblemResponses.Status409
    ConsultationOfferingResponse removeConsultationOfferingImage(@PathVariable UUID id) {
        return ConsultationOfferingResponse.from(removeConsultationOfferingImage.invoke(new OfferingId(id)));
    }

    /** A value object that rejects a value Bean Validation let through throws, and the API answers 400. */
    private static ConsultationOfferingCommand toCommand(ConsultationOfferingRequest request) {
        return new ConsultationOfferingCommand(new OfferingName(request.name()), request.description(),
                new SessionDuration(request.sessionDurationMinutes()),
                new CancellationDeadline(request.cancellationDeadlineHours()),
                request.packages().stream().map(ConsultationOfferingController::toEntry).toList(),
                request.eligibleExpertIds().stream().map(UserId::new).collect(Collectors.toSet()));
    }

    private static ConsultationPackageEntry toEntry(ConsultationPackageRequest request) {
        SessionCount sessionCount = new SessionCount(request.sessionCount());
        Money price = Money.chf(request.price());
        return request.id() == null ? ConsultationPackageEntry.added(sessionCount, price)
                : ConsultationPackageEntry.kept(new ConsultationPackageId(request.id()), sessionCount, price);
    }
}
