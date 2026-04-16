package com.ebikes.assignments.services.offers;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.DomainEvents;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Offer;
import com.ebikes.assignments.database.repositories.OfferRepository;
import com.ebikes.assignments.dtos.events.outgoing.OfferAcceptedEvent;
import com.ebikes.assignments.dtos.events.outgoing.OfferDeclinedEvent;
import com.ebikes.assignments.dtos.responses.offers.OfferResponse;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.BusinessRuleException;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.OfferMapper;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.audit.AuditTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OfferService {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final AuditTemplate auditTemplate;
  private final OfferMapper offerMapper;
  private final OfferRepository offerRepository;
  private final OutboxService outboxService;

  @Transactional
  public OfferResponse accept(UUID offerId, String agentId) {
    Offer offer = requireById(offerId);
    validateOwnership(offer, agentId);

    String organizationId = offer.getAssignment().getOrderContext().getOrganizationId();

    auditTemplate.execute(
        offer,
        organizationId,
        DomainEvents.Offers.ACCEPTED,
        () -> {
          offer.accept();
          offerRepository.save(offer);
        });

    outboxService.publish(
        DomainEvents.Offers.ACCEPTED,
        new OfferAcceptedEvent(offer.getAgentId(), offer.getAssignment().getId(), offer.getId()),
        RoutingKeys.ASSIGNMENT_OFFER_ACCEPTED);

    applicationEventPublisher.publishEvent(offerMapper.toAcceptedTrigger(offer));

    log.info(
        "Offer accepted: offerId={}, assignmentId={}, agentId={}",
        offerId,
        offer.getAssignment().getId(),
        agentId);

    return offerMapper.toResponse(offer);
  }

  @Transactional
  public void cancelActiveOffers(Assignment assignment) {
    List<Offer> activeOffers =
        offerRepository.findByAssignmentId(assignment.getId()).stream()
            .filter(o -> !o.getStatus().isTerminal())
            .toList();

    activeOffers.forEach(
        offer -> {
          offer.cancel();
          outboxService.publish(
              DomainEvents.Offers.CANCELLED,
              offerMapper.toCancelledEvent(offer),
              RoutingKeys.ASSIGNMENT_OFFER_CANCELLED);
        });

    offerRepository.saveAll(activeOffers);
  }

  @Transactional
  public Offer create(String agentId, Assignment assignment, OffsetDateTime expiresAt) {
    Set<String> exhaustedAgentIds = requireExhaustedAgents(assignment.getId());

    if (exhaustedAgentIds.contains(agentId)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Agent already reached a terminal offer result for this attempt: " + agentId);
    }

    Offer offer =
        Offer.builder().agentId(agentId).assignment(assignment).expiresAt(expiresAt).build();

    return offerRepository.save(offer);
  }

  @Transactional
  public OfferResponse decline(UUID offerId, String agentId) {
    Offer offer = requireById(offerId);
    validateOwnership(offer, agentId);

    String organizationId = offer.getAssignment().getOrderContext().getOrganizationId();

    auditTemplate.execute(
        offer,
        organizationId,
        DomainEvents.Offers.DECLINED,
        () -> {
          offer.decline();
          offerRepository.save(offer);
        });

    outboxService.publish(
        DomainEvents.Offers.DECLINED,
        new OfferDeclinedEvent(offer.getAgentId(), offer.getAssignment().getId(), offer.getId()),
        RoutingKeys.ASSIGNMENT_OFFER_DECLINED);

    applicationEventPublisher.publishEvent(offerMapper.toDeclinedTrigger(offer));

    log.info(
        "Offer declined: offerId={}, assignmentId={}, agentId={}",
        offerId,
        offer.getAssignment().getId(),
        agentId);

    return offerMapper.toResponse(offer);
  }

  @Transactional(readOnly = true)
  public boolean hasLiveOffer(UUID assignmentId) {
    return offerRepository.existsByAssignmentIdAndStatus(assignmentId, OfferStatus.CREATED);
  }

  @Transactional(readOnly = true)
  public Set<String> getExhaustedAgentIds(UUID assignmentId) {
    return requireExhaustedAgents(assignmentId);
  }

  private Offer requireById(UUID offerId) {
    return offerRepository
        .findById(offerId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Offer not found: " + offerId));
  }

  private Set<String> requireExhaustedAgents(UUID assignmentId) {
    return offerRepository.findExhaustedAgentIds(
        assignmentId,
        Arrays.stream(OfferStatus.values())
            .filter(OfferStatus::isExhausted)
            .collect(Collectors.toSet()));
  }

  private void validateOwnership(Offer offer, String agentId) {
    if (!offer.getAgentId().equals(agentId)) {
      throw new BusinessRuleException(ResponseCode.FORBIDDEN, "Agent does not own this offer");
    }
  }
}
