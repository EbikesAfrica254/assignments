package com.ebikes.assignments.services.assignments.strategies;

import java.util.List;

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
import com.ebikes.assignments.services.shortlist.RankingService;
import com.ebikes.assignments.services.shortlist.ShortlistService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankedStrategy implements Strategy {

  private final OfferMapper offerMapper;
  private final OfferService offerService;
  private final OfferExpiryService offerExpiryService;
  private final OutboxService outboxService;
  private final RankingService rankingService;
  private final ShortlistService shortlistService;

  @Override
  @Transactional
  public boolean execute(Assignment assignment, OrderContext orderContext) {
    if (!rankingService.isRanked(assignment.getId())) {
      rankingService.rank(assignment, orderContext);
    }

    List<Shortlist> eligible =
        shortlistService.findRankedEligible(
            assignment.getId(), offerService.getExhaustedAgentIds(assignment.getId()));

    if (eligible.isEmpty()) {
      log.debug("All ranked candidates exhausted: assignmentId={}", assignment.getId());
      return false;
    }

    Offer offer =
        offerService.create(
            eligible.getFirst().getAgentId(), assignment, offerExpiryService.offerExpiresAt());

    outboxService.publish(
        DomainEvents.Offers.CREATED,
        offerMapper.toCreatedEvent(offer),
        RoutingKeys.ASSIGNMENT_OFFER_CREATED);

    log.info(
        "RANKED offer created: assignmentId={}, agentId={}",
        assignment.getId(),
        eligible.getFirst().getAgentId());

    return true;
  }

  @Override
  public AssignmentStrategy supports() {
    return AssignmentStrategy.RANKED;
  }
}
