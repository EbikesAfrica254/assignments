package com.ebikes.assignments.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.assignments.dtos.responses.api.SuccessResponse;
import com.ebikes.assignments.dtos.responses.offers.OfferResponse;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/assignment-offers")
@RestController
public class OfferController {

  private final OfferService offerService;

  @PreAuthorize("hasAuthority('AGENT')")
  @PostMapping("/{offerId}/accept")
  public ResponseEntity<SuccessResponse<OfferResponse>> accept(@PathVariable UUID offerId) {
    return ResponseEntity.ok(
        SuccessResponse.of(
            offerService.accept(offerId, ExecutionContext.getUserId()), "Offer accepted"));
  }

  @PreAuthorize("hasAuthority('AGENT')")
  @PostMapping("/{offerId}/decline")
  public ResponseEntity<SuccessResponse<OfferResponse>> decline(@PathVariable UUID offerId) {
    return ResponseEntity.ok(
        SuccessResponse.of(
            offerService.decline(offerId, ExecutionContext.getUserId()), "Offer declined"));
  }
}
