package com.ebikes.assignments.services.assignments;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.configurations.properties.AssignmentProperties;
import com.ebikes.assignments.constants.EventConstants.EventTypes;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.database.repositories.AssignmentOfferRepository;
import com.ebikes.assignments.dtos.events.outgoing.OfferExpiredEvent;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.publishers.AuditEventPublisher;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.audit.AuditMetadataBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OfferExpiryService {

  private final AssignmentOfferRepository assignmentOfferRepository;
  private final AssignmentOrchestrationService assignmentOrchestrationService;
  private final AssignmentProperties assignmentProperties;
  private final AuditEventPublisher auditEventPublisher;
  private final OutboxService outboxService;

  @Transactional
  public void processOffer(AssignmentOffer offer) {
    try {
      offer.expire();
      assignmentOfferRepository.save(offer);

      outboxService.save(
          EventTypes.AssignmentOffers.EXPIRED,
          new OfferExpiredEvent(offer.getAgentId(), offer.getAssignment().getId(), offer.getId()),
          RoutingKeys.ASSIGNMENT_OFFER_EXPIRED);

      auditEventPublisher.publishSuccess(
          offer.getId(),
          AssignmentOffer.class.getSimpleName(),
          EventTypes.AssignmentOffers.EXPIRED,
          AuditMetadataBuilder.forOffer(offer),
          offer.getAssignment().getOrderContext().getOrganizationId(),
          RoutingKeys.ASSIGNMENT_OFFER_AUDIT);

      assignmentOrchestrationService.publishOfferTerminated(
          offer.getAssignment().getId(), offer.getAssignment().getStrategy());

      log.info(
          "Offer expired: offerId={}, assignmentId={}",
          offer.getId(),
          offer.getAssignment().getId());

    } catch (Exception e) {
      log.error(
          "Failed to expire offer: offerId={}, assignmentId={} — recording failure",
          offer.getId(),
          offer.getAssignment().getId(),
          e);

      try {
        recordExpiryFailure(offer);
      } catch (Exception failureRecordingException) {
        log.error(
            "Failed to record expiry failure for offerId={}",
            offer.getId(),
            failureRecordingException);
      }
    }
  }

  public void recordExpiryFailure(AssignmentOffer offer) {
    offer.recordExpiryFailure(assignmentProperties.getOfferExpiryMaxAttempts());
    assignmentOfferRepository.save(offer);

    if (offer.getStatus() == OfferStatus.EXPIRY_FAILED) {
      auditEventPublisher.publishFailure(
          offer.getId(),
          AssignmentOffer.class.getSimpleName(),
          EventTypes.AssignmentOffers.EXPIRED,
          "Offer expiry failed after "
              + assignmentProperties.getOfferExpiryMaxAttempts()
              + " attempts",
          AuditMetadataBuilder.forOffer(offer),
          offer.getAssignment().getOrderContext().getOrganizationId(),
          RoutingKeys.ASSIGNMENT_OFFER_AUDIT);

      log.error(
          "Offer dead-lettered after {} failed expiry attempts: offerId={}, assignmentId={}",
          assignmentProperties.getOfferExpiryMaxAttempts(),
          offer.getId(),
          offer.getAssignment().getId());
    }
  }
}
