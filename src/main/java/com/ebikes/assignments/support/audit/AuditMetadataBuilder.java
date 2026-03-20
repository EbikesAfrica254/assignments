package com.ebikes.assignments.support.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.AssignmentOffer;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditMetadataBuilder {

  public static Map<String, String> forAssignment(Assignment assignment) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("assignmentId", assignment.getId().toString());
    metadata.put("orderId", assignment.getOrderContext().getOrderId().toString());
    metadata.put("organizationId", assignment.getOrderContext().getOrganizationId());
    metadata.put("status", assignment.getStatus().name());
    metadata.put("strategy", assignment.getStrategy().name());
    if (assignment.getOrderContext().getBranchId() != null) {
      metadata.put("branchId", assignment.getOrderContext().getBranchId());
    }
    if (assignment.getWinnerAgentId() != null) {
      metadata.put("winnerAgentId", assignment.getWinnerAgentId());
    }
    if (assignment.getFailureReason() != null) {
      metadata.put("failureReason", assignment.getFailureReason());
    }
    if (assignment.getCancellationReason() != null) {
      metadata.put("cancellationReason", assignment.getCancellationReason());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forOffer(AssignmentOffer offer) {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("agentId", offer.getAgentId());
    metadata.put("assignmentId", offer.getAssignment().getId().toString());
    metadata.put("expiresAt", offer.getExpiresAt().toString());
    metadata.put("offerId", offer.getId().toString());
    metadata.put("status", offer.getStatus().name());
    if (offer.getRespondedAt() != null) {
      metadata.put("respondedAt", offer.getRespondedAt().toString());
    }
    return Collections.unmodifiableMap(metadata);
  }
}
