package com.ebikes.assignments.services.assignments.strategies;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.configurations.properties.ServiceProperties;
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
public class BroadcastStrategy implements Strategy {

  private final ServiceProperties serviceProperties;
  private final OfferMapper offerMapper;
  private final OfferService offerService;
  private final OfferExpiryService offerExpiryService;
  private final OutboxService outboxService;
  private final ShortlistService shortlistService;

  @Override
  @Transactional
  public boolean execute(Assignment assignment, OrderContext orderContext) {
    List<Shortlist> eligible =
        shortlistService.findEligible(
            assignment.getId(),
            offerService.getExhaustedAgentIds(assignment.getId()),
            serviceProperties.getCandidates().getBroadcastMax());

    if (eligible.isEmpty()) {
      log.debug("All broadcast candidates exhausted: assignmentId={}", assignment.getId());
      return false;
    }

    List<Offer> offers =
        eligible.stream()
            .map(
                c ->
                    offerService.create(
                        c.getAgentId(), assignment, offerExpiryService.offerExpiresAt()))
            .toList();

    outboxService.bulkPublish(
        DomainEvents.Offers.CREATED,
        offers.stream().map(offerMapper::toCreatedEvent).toList(),
        RoutingKeys.ASSIGNMENT_OFFER_CREATED);

    log.info(
        "BROADCAST offers created: assignmentId={}, offerCount={}",
        assignment.getId(),
        offers.size());

    return true;
  }

  @Override
  public AssignmentStrategy supports() {
    return AssignmentStrategy.BROADCAST;
  }
}
