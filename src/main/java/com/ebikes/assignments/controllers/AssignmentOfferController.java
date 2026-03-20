package com.ebikes.assignments.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.assignments.dtos.responses.api.SuccessResponse;
import com.ebikes.assignments.dtos.responses.offers.AssignmentOfferResponse;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.enums.UserRole;
import com.ebikes.assignments.exceptions.AuthorizationException;
import com.ebikes.assignments.services.assignments.AssignmentOfferService;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/assignment-offers")
@RestController
public class AssignmentOfferController {

  private final AssignmentOfferService assignmentOfferService;

  @PostMapping("/{offerId}/accept")
  public ResponseEntity<SuccessResponse<AssignmentOfferResponse>> accept(
      @PathVariable UUID offerId) {
    enforceAgentRole();
    return ResponseEntity.ok(
        SuccessResponse.of(
            assignmentOfferService.acceptOffer(offerId, ExecutionContext.getUserId()),
            "Offer accepted"));
  }

  @PostMapping("/{offerId}/decline")
  public ResponseEntity<SuccessResponse<AssignmentOfferResponse>> decline(
      @PathVariable UUID offerId) {
    enforceAgentRole();
    return ResponseEntity.ok(
        SuccessResponse.of(
            assignmentOfferService.declineOffer(offerId, ExecutionContext.getUserId()),
            "Offer declined"));
  }

  private void enforceAgentRole() {
    if (!ExecutionContext.getRoles().contains(UserRole.AGENT.name())) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Access denied: agent role required");
    }
  }
}
