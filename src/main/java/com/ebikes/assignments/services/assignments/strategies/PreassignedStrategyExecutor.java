package com.ebikes.assignments.services.assignments.strategies;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PreassignedStrategyExecutor implements AssignmentStrategyExecutor {

  private final AssignmentProperties assignmentProperties;
  private final AssignmentRepository assignmentRepository;
  private final OutboxService outboxService;
  private final ShortlistCandidateRepository shortlistCandidateRepository;

  @Override
  public boolean execute(Assignment assignment, OrderContext orderContext) {
    List<ShortlistCandidate> candidates =
        shortlistCandidateRepository.findByAssignmentIdOrderByRankAsc(assignment.getId());

    Optional<ShortlistCandidate> preferred =
        candidates.stream().filter(ShortlistCandidate::isPreferred).findFirst();

    if (preferred.isEmpty()) {
      log.debug(
          "No preferred candidate in shortlist for PREASSIGNED: assignmentId={}",
          assignment.getId());
      return false;
    }

    AssignmentOffer offer = assignment.createOffer(preferred.get().getAgentId(), offerExpiresAt());
    assignment.awaitResponse();
    assignmentRepository.save(assignment);

    outboxService.save(
        EventTypes.AssignmentOffers.CREATED,
        new OfferCreatedEvent(
            offer.getAgentId(), assignment.getId(), offer.getExpiresAt(), offer.getId()),
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

  private OffsetDateTime offerExpiresAt() {
    return OffsetDateTime.now(ZoneOffset.UTC)
        .plusMinutes(assignmentProperties.getOfferExpiryMinutes());
  }
}
