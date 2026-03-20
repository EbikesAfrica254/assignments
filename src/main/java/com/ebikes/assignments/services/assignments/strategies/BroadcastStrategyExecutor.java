package com.ebikes.assignments.services.assignments.strategies;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ebikes.assignments.configurations.properties.AssignmentProperties;
import com.ebikes.assignments.constants.EventConstants.EventTypes;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.ShortlistCandidate;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.database.repositories.ShortlistCandidateRepository;
import com.ebikes.assignments.dtos.events.outgoing.OfferCreatedEvent;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class BroadcastStrategyExecutor implements AssignmentStrategyExecutor {

  private final AssignmentProperties assignmentProperties;
  private final AssignmentRepository assignmentRepository;
  private final OutboxService outboxService;
  private final ShortlistCandidateRepository shortlistCandidateRepository;

  @Override
  public boolean execute(Assignment assignment, OrderContext orderContext) {
    List<ShortlistCandidate> candidates =
        shortlistCandidateRepository.findByAssignmentIdOrderByRankAsc(assignment.getId());

    if (candidates.isEmpty()) {
      log.debug("No candidates available for BROADCAST: assignmentId={}", assignment.getId());
      return false;
    }

    Set<String> exhaustedAgentIds = getExhaustedAgentIds(assignment);

    List<ShortlistCandidate> eligible =
        candidates.stream()
            .filter(c -> !exhaustedAgentIds.contains(c.getAgentId()))
            .limit(assignmentProperties.getBroadcastMaxCandidates())
            .toList();

    if (eligible.isEmpty()) {
      log.debug("All broadcast candidates exhausted: assignmentId={}", assignment.getId());
      return false;
    }

    List<AssignmentOffer> offers =
        eligible.stream()
            .map(c -> assignment.createOffer(c.getAgentId(), offerExpiresAt()))
            .toList();

    if (assignment.getStatus() == AssignmentStatus.STARTED) {
      assignment.awaitResponse();
    }

    assignmentRepository.save(assignment);

    offers.forEach(
        offer ->
            outboxService.save(
                EventTypes.AssignmentOffers.CREATED,
                new OfferCreatedEvent(
                    offer.getAgentId(), assignment.getId(), offer.getExpiresAt(), offer.getId()),
                RoutingKeys.ASSIGNMENT_OFFER_CREATED));

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

  private Set<String> getExhaustedAgentIds(Assignment assignment) {
    return assignment.getOffers().stream()
        .filter(o -> o.getStatus() != OfferStatus.CANCELLED && o.getStatus().isTerminal())
        .map(AssignmentOffer::getAgentId)
        .collect(Collectors.toSet());
  }

  private OffsetDateTime offerExpiresAt() {
    return OffsetDateTime.now(ZoneOffset.UTC)
        .plusMinutes(assignmentProperties.getOfferExpiryMinutes());
  }
}
