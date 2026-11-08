package com.cleanlearning.api.offering;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.offering.response.ConsultationOfferingSummaryResponse;
import com.cleanlearning.application.offering.ListConsultationOfferingsUseCase;

/**
 * The admin's consultation offerings (UC-16).
 */
@RestController
@RequestMapping("/api/admin/consultation-offerings")
class ConsultationOfferingController {

    private final ListConsultationOfferingsUseCase listConsultationOfferings;

    ConsultationOfferingController(ListConsultationOfferingsUseCase listConsultationOfferings) {
        this.listConsultationOfferings = listConsultationOfferings;
    }

    @GetMapping
    List<ConsultationOfferingSummaryResponse> list() {
        return listConsultationOfferings.invoke().stream().map(ConsultationOfferingSummaryResponse::from).toList();
    }
}
