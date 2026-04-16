package com.ebikes.assignments.services.assignments.strategies;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.DomainEvents;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Offer;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.mappers.OfferMapper;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.services.offers.OfferExpiryService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.services.shortlist.ShortlistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreassignedStrategy implements Strategy {

  private final OfferMapper offerMapper;
  private final OfferService offerService;
  private final OfferExpiryService offerExpiryService;
  private final OutboxService outboxService;
  private final ShortlistService shortlistService;

  @Override
  @Transactional
  public boolean execute(Assignment assignment, OrderContext orderContext) {
    Optional<Shortlist> preferred = shortlistService.findPreferred(assignment.getId());

    if (preferred.isEmpty()) {
      log.debug(
          "No preferred candidate in shortlist for PREASSIGNED: assignmentId={}",
          assignment.getId());
      return false;
    }

    Set<String> exhausted = offerService.getExhaustedAgentIds(assignment.getId());

    if (exhausted.contains(preferred.get().getAgentId())) {
      log.debug(
          "Preferred candidate is exhausted for PREASSIGNED: assignmentId={}, agentId={}",
          assignment.getId(),
          preferred.get().getAgentId());
      return false;
    }

    Offer offer =
        offerService.create(
            preferred.get().getAgentId(), assignment, offerExpiryService.offerExpiresAt());

    outboxService.publish(
        DomainEvents.Offers.CREATED,
        offerMapper.toCreatedEvent(offer),
        RoutingKeys.ASSIGNMENT_OFFER_CREATED);

    log.info(
        "PREASSIGNED offer created: assignmentId={}, agentId={}",
        assignment.getId(),
        preferred.get().getAgentId());

    return true;
  }

  @Override
  public AssignmentStrategy supports() {
    return AssignmentStrategy.PREASSIGNED;
  }
}
