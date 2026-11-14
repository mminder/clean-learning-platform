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

/**
 * The customer's cases (UC-03, UC-05), their bookings (UC-06), their appointments' cancellations
 * (UC-07) and their cancellations (UC-08). Security lets only users with {@code CUSTOMER} in; each
 * use case is scoped to the current user, so another customer's case answers 404.
 */
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

  /** UC-05's landing page: the current user's own cases, in the order of "My purchases". */
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  List<CustomerConsultationCaseSummaryResponse> listCustomerConsultationCases(
      @AuthenticationPrincipal CurrentUser currentUser) {
    return listCustomerConsultationCases.invoke(currentUser).stream()
        .map(CustomerConsultationCaseSummaryResponse::from)
        .toList();
  }

  /**
   * UC-03: buys a package; the current user is the case's customer. {@code @ResponseStatus} tells
   * springdoc the status that {@code ResponseEntity.created} sets, and {@code @ApiResponse} adds
   * its {@code Location} header.
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

  /** UC-06 step 2: the experts the first booking can pick, by display name. */
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

  /**
   * UC-06 steps 3–4: the times the case can book with the expert, soonest first. {@code expertId}
   * is the expert the dialog shows; without it Spring answers 400. A cancelled case answers 409
   * {@code CASE_CANCELLED}.
   */
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

  /**
   * UC-06 steps 6–7: books a session on the case and answers the new appointment. No {@code
   * Location}: an appointment has no route of its own; it's part of its case, which the page
   * reloads (spec 04, decision 17).
   */
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
   * UC-07 steps 3–5: cancels an appointment of the case with the outcome the dialog stated, and
   * answers the case without it (spec 05, decision 12). The appointment is addressed under its
   * case, so the case's scoped query decides ownership, and an appointment of another case answers
   * 404.
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

  /**
   * UC-08 steps 3–5: cancels the case, and with it its appointments that haven't started, with the
   * outcome the dialog stated, and answers the cancelled case (spec 05, decision 12).
   */
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

  /**
   * A value object that rejects a value Bean Validation let through throws, and the API answers
   * 400.
   */
  private static PurchaseConsultationPackageCommand toCommand(
      PurchaseConsultationPackageRequest request) {
    return new PurchaseConsultationPackageCommand(
        new OfferingId(request.offeringId()),
        new ConsultationPackageId(request.packageId()),
        Money.chf(request.price()),
        new SessionCount(request.sessionCount()));
  }
}
