package com.ebikes.assignments.services.assignments;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.EventSource;
import com.ebikes.assignments.constants.EventConstants.EventTypes;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.database.repositories.AssignmentOfferRepository;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentSucceededEvent;
import com.ebikes.assignments.dtos.events.outgoing.OfferAcceptedEvent;
import com.ebikes.assignments.dtos.events.outgoing.OfferCancelledEvent;
import com.ebikes.assignments.dtos.events.outgoing.OfferDeclinedEvent;
import com.ebikes.assignments.dtos.responses.offers.AssignmentOfferResponse;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.BusinessRuleException;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.AssignmentOfferMapper;
import com.ebikes.assignments.publishers.AuditEventPublisher;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.audit.AuditMetadataBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AssignmentOfferService {

  private final AssignmentOfferMapper assignmentOfferMapper;
  private final AssignmentOfferRepository assignmentOfferRepository;
  private final AssignmentOrchestrationService assignmentOrchestrationService;
  private final AssignmentRepository assignmentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final OutboxService outboxService;

  @Transactional
  public AssignmentOfferResponse acceptOffer(UUID offerId, String agentId) {
    AssignmentOffer offer = requireById(offerId);
    validateOwnership(offer, agentId);

    Assignment assignment =
        assignmentRepository
            .findByIdWithPessimisticLock(offer.getAssignment().getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "Assignment not found for offer: " + offerId));

    AssignmentOffer lockedOffer =
        assignment.getOffers().stream()
            .filter(o -> o.getId().equals(offerId))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Offer not found on assignment after lock: " + offerId));

    if (assignment.getStatus() == AssignmentStatus.SUCCEEDED) {
      lockedOffer.cancel();
      assignmentRepository.save(assignment);

      outboxService.save(
          EventTypes.AssignmentOffers.CANCELLED,
          new OfferCancelledEvent(
              lockedOffer.getAgentId(), assignment.getId(), lockedOffer.getId()),
          RoutingKeys.ASSIGNMENT_OFFER_CANCELLED);

      log.info(
          "Late accept rejected — assignment already succeeded: offerId={}, assignmentId={}",
          offerId,
          assignment.getId());

      return assignmentOfferMapper.toResponse(lockedOffer);
    }

    lockedOffer.accept();
    assignment.succeed(agentId);

    List<AssignmentOffer> cancelledOffers =
        assignment.getOffers().stream()
            .filter(o -> !o.getId().equals(offerId) && o.getStatus() == OfferStatus.CREATED)
            .toList();

    cancelledOffers.forEach(AssignmentOffer::cancel);

    assignmentRepository.save(assignment);

    outboxService.save(
        EventTypes.AssignmentOffers.ACCEPTED,
        new OfferAcceptedEvent(lockedOffer.getAgentId(), assignment.getId(), lockedOffer.getId()),
        RoutingKeys.ASSIGNMENT_OFFER_ACCEPTED);

    cancelledOffers.forEach(
        cancelled ->
            outboxService.save(
                EventTypes.AssignmentOffers.CANCELLED,
                new OfferCancelledEvent(
                    cancelled.getAgentId(), assignment.getId(), cancelled.getId()),
                RoutingKeys.ASSIGNMENT_OFFER_CANCELLED));

    outboxService.save(
        EventTypes.Assignments.SUCCEEDED,
        new AssignmentSucceededEvent(
            assignment.getId(),
            assignment.getOrderContext().getOrderId(),
            assignment.getOrderContext().getOrganizationId(),
            EventSource.serviceReference(),
            agentId),
        RoutingKeys.ASSIGNMENT_SUCCEEDED);

    auditEventPublisher.publishSuccess(
        assignment.getId(),
        Assignment.class.getSimpleName(),
        EventTypes.Assignments.SUCCEEDED,
        AuditMetadataBuilder.forAssignment(assignment),
        RoutingKeys.ASSIGNMENT_AUDIT);

    log.info(
        "Offer accepted, assignment succeeded: offerId={}, assignmentId={}, agentId={}",
        offerId,
        assignment.getId(),
        agentId);

    return assignmentOfferMapper.toResponse(lockedOffer);
  }

  @Transactional
  public AssignmentOfferResponse declineOffer(UUID offerId, String agentId) {
    AssignmentOffer offer = requireById(offerId);
    validateOwnership(offer, agentId);

    if (offer.getStatus() != OfferStatus.CREATED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Offer is not in a declinable state: " + offer.getStatus());
    }

    offer.decline();
    assignmentOfferRepository.save(offer);

    outboxService.save(
        EventTypes.AssignmentOffers.DECLINED,
        new OfferDeclinedEvent(offer.getAgentId(), offer.getAssignment().getId(), offer.getId()),
        RoutingKeys.ASSIGNMENT_OFFER_DECLINED);

    auditEventPublisher.publishSuccess(
        offer.getId(),
        AssignmentOffer.class.getSimpleName(),
        EventTypes.AssignmentOffers.DECLINED,
        AuditMetadataBuilder.forOffer(offer),
        offer.getAssignment().getOrderContext().getOrganizationId(),
        RoutingKeys.ASSIGNMENT_OFFER_AUDIT);

    assignmentOrchestrationService.publishOfferTerminated(
        offer.getAssignment().getId(), offer.getAssignment().getStrategy());

    log.info(
        "Offer declined: offerId={}, assignmentId={}, agentId={}",
        offerId,
        offer.getAssignment().getId(),
        agentId);

    return assignmentOfferMapper.toResponse(offer);
  }

  private AssignmentOffer requireById(UUID offerId) {
    return assignmentOfferRepository
        .findById(offerId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Offer not found: " + offerId));
  }

  private void validateOwnership(AssignmentOffer offer, String agentId) {
    if (!offer.getAgentId().equals(agentId)) {
      throw new BusinessRuleException(
          ResponseCode.FORBIDDEN, "Agent does not own this offer: " + offer.getId());
    }
  }
}
