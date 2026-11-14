package com.cleanlearning.api.consultation;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.consultation.request.BookConsultationAppointmentRequest;
import com.cleanlearning.api.consultation.request.CancelConsultationAppointmentRequest;
import com.cleanlearning.api.consultation.request.CancelConsultationCaseRequest;
import com.cleanlearning.api.consultation.request.PurchaseConsultationPackageRequest;
import com.cleanlearning.api.consultation.response.BookableSlotResponse;
import com.cleanlearning.api.consultation.response.CustomerAppointmentResponse;
import com.cleanlearning.api.consultation.response.CustomerConsultationCaseResponse;
import com.cleanlearning.api.consultation.response.CustomerConsultationCaseSummaryResponse;
import com.cleanlearning.api.response.EligibleExpertResponse;
import com.cleanlearning.application.consultation.BookConsultationAppointmentCommand;
import com.cleanlearning.application.consultation.BookConsultationAppointmentUseCase;
import com.cleanlearning.application.consultation.CancelConsultationAppointmentCommand;
import com.cleanlearning.application.consultation.CancelConsultationAppointmentUseCase;
import com.cleanlearning.application.consultation.CancelConsultationCaseCommand;
import com.cleanlearning.application.consultation.CancelConsultationCaseUseCase;
import com.cleanlearning.application.consultation.GetCustomerConsultationCaseUseCase;
import com.cleanlearning.application.consultation.ListBookableSlotsUseCase;
import com.cleanlearning.application.consultation.ListCustomerConsultationCasesUseCase;
import com.cleanlearning.application.consultation.ListEligibleExpertsForCaseUseCase;
import com.cleanlearning.application.consultation.PurchaseConsultationPackageCommand;
import com.cleanlearning.application.consultation.PurchaseConsultationPackageUseCase;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.value.UserId;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(CustomerConsultationCaseController.PATH)
@PreAuthorize("hasRole('CUSTOMER')")
class CustomerConsultationCaseController {

  static final String PATH = "/api/customer/consultation-cases";

  private final PurchaseConsultationPackageUseCase purchaseConsultationPackage;

  private final GetCustomerConsultationCaseUseCase getCustomerConsultationCase;

  private final ListCustomerConsultationCasesUseCase listCustomerConsultationCases;

  private final ListEligibleExpertsForCaseUseCase listEligibleExpertsForCase;

  private final ListBookableSlotsUseCase listBookableSlots;

  private final BookConsultationAppointmentUseCase bookConsultationAppointment;

  private final CancelConsultationAppointmentUseCase cancelConsultationAppointment;

  private final CancelConsultationCaseUseCase cancelConsultationCase;

  CustomerConsultationCaseController(
      PurchaseConsultationPackageUseCase purchaseConsultationPackage,
      GetCustomerConsultationCaseUseCase getCustomerConsultationCase,
      ListCustomerConsultationCasesUseCase listCustomerConsultationCases,
      ListEligibleExpertsForCaseUseCase listEligibleExpertsForCase,
      ListBookableSlotsUseCase listBookableSlots,
      BookConsultationAppointmentUseCase bookConsultationAppointment,
      CancelConsultationAppointmentUseCase cancelConsultationAppointment,
      CancelConsultationCaseUseCase cancelConsultationCase) {
    this.purchaseConsultationPackage = purchaseConsultationPackage;
    this.getCustomerConsultationCase = getCustomerConsultationCase;
    this.listCustomerConsultationCases = listCustomerConsultationCases;
    this.listEligibleExpertsForCase = listEligibleExpertsForCase;
    this.listBookableSlots = listBookableSlots;
    this.bookConsultationAppointment = bookConsultationAppointment;
    this.cancelConsultationAppointment = cancelConsultationAppointment;
    this.cancelConsultationCase = cancelConsultationCase;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  List<CustomerConsultationCaseSummaryResponse> listCustomerConsultationCases(
      @AuthenticationPrincipal CurrentUser currentUser) {
    return listCustomerConsultationCases.invoke(currentUser).stream()
        .map(CustomerConsultationCaseSummaryResponse::from)
        .toList();
  }

  /**
   * {@code @ResponseStatus} tells springdoc the status that {@code ResponseEntity.created} sets,
   * and {@code @ApiResponse} adds its {@code Location} header.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @ApiResponse(
      responseCode = "201",
      headers =
          @Header(
              name = HttpHeaders.LOCATION,
              description = "The new case's URL, relative",
              schema = @Schema(type = "string", format = "uri-reference")))
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status409
  @ProblemResponses.Status415
  ResponseEntity<CustomerConsultationCaseResponse> purchaseConsultationPackage(
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody PurchaseConsultationPackageRequest request) {
    CustomerConsultationCase consultationCase =
        purchaseConsultationPackage.invoke(currentUser, toCommand(request));
    return ResponseEntity.created(URI.create(PATH + "/" + consultationCase.id().value()))
        .body(CustomerConsultationCaseResponse.from(consultationCase));
  }

  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  CustomerConsultationCaseResponse getCustomerConsultationCase(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
    return CustomerConsultationCaseResponse.from(
        getCustomerConsultationCase.invoke(currentUser, new ConsultationCaseId(id)));
  }

  @GetMapping("/{id}/eligible-experts")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  List<EligibleExpertResponse> listEligibleExpertsForCase(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
    return listEligibleExpertsForCase.invoke(currentUser, new ConsultationCaseId(id)).stream()
        .map(EligibleExpertResponse::from)
        .toList();
  }

  @GetMapping("/{id}/bookable-slots")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status409
  List<BookableSlotResponse> listBookableSlots(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @RequestParam UUID expertId) {
    return listBookableSlots
        .invoke(currentUser, new ConsultationCaseId(id), new UserId(expertId))
        .stream()
        .map(BookableSlotResponse::from)
        .toList();
  }

  /** No {@code Location}: an appointment has no route of its own; it's part of its case. */
  @PostMapping("/{id}/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status409
  @ProblemResponses.Status415
  CustomerAppointmentResponse bookConsultationAppointment(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @Valid @RequestBody BookConsultationAppointmentRequest request) {
    return CustomerAppointmentResponse.from(
        bookConsultationAppointment.invoke(
            currentUser,
            new BookConsultationAppointmentCommand(
                new ConsultationCaseId(id), new UserId(request.expertId()), request.start())));
  }

  /**
   * The appointment is addressed under its case, so the case's scoped query decides ownership, and
   * an appointment of another case answers 404.
   */
  @PostMapping("/{id}/appointments/{appointmentId}/cancel")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status409
  @ProblemResponses.Status415
  CustomerConsultationCaseResponse cancelConsultationAppointment(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @PathVariable UUID appointmentId,
      @Valid @RequestBody CancelConsultationAppointmentRequest request) {
    return CustomerConsultationCaseResponse.from(
        cancelConsultationAppointment.invoke(
            currentUser,
            new CancelConsultationAppointmentCommand(
                new ConsultationCaseId(id),
                new AppointmentId(appointmentId),
                request.creditRefunded())));
  }

  @PostMapping("/{id}/cancel")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status409
  @ProblemResponses.Status415
  CustomerConsultationCaseResponse cancelConsultationCase(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @Valid @RequestBody CancelConsultationCaseRequest request) {
    return CustomerConsultationCaseResponse.from(
        cancelConsultationCase.invoke(
            currentUser,
            new CancelConsultationCaseCommand(
                new ConsultationCaseId(id), request.refundEligible())));
  }

  private static PurchaseConsultationPackageCommand toCommand(
      PurchaseConsultationPackageRequest request) {
    return new PurchaseConsultationPackageCommand(
        new OfferingId(request.offeringId()),
        new ConsultationPackageId(request.packageId()),
        Money.chf(request.price()),
        new SessionCount(request.sessionCount()));
  }
}
