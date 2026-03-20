package com.ebikes.assignments.services.assignments.strategies;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ebikes.assignments.adapters.RoutingServiceClient;
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
import com.ebikes.assignments.dtos.internal.MatrixEntryDto;
import com.ebikes.assignments.dtos.internal.MatrixResponseDto;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankedStrategyExecutor implements AssignmentStrategyExecutor {

  private final AssignmentProperties assignmentProperties;
  private final AssignmentRepository assignmentRepository;
  private final OutboxService outboxService;
  private final RoutingServiceClient assignmentsServiceClient;
  private final ShortlistCandidateRepository shortlistCandidateRepository;

  @Override
  public boolean execute(Assignment assignment, OrderContext orderContext) {
    List<ShortlistCandidate> candidates =
        shortlistCandidateRepository.findByAssignmentIdOrderByRankAsc(assignment.getId());

    if (candidates.isEmpty()) {
      log.debug("No candidates available for RANKED: assignmentId={}", assignment.getId());
      return false;
    }

    boolean ranksAssigned = candidates.stream().anyMatch(c -> c.getRank() != null);
    if (!ranksAssigned) {
      assignRanksFromMatrix(assignment, orderContext, candidates);
      candidates =
          shortlistCandidateRepository.findByAssignmentIdOrderByRankAsc(assignment.getId());
    }

    Set<String> exhaustedAgentIds = getExhaustedAgentIds(assignment);

    Optional<ShortlistCandidate> next =
        candidates.stream().filter(c -> !exhaustedAgentIds.contains(c.getAgentId())).findFirst();

    if (next.isEmpty()) {
      log.debug("All ranked candidates exhausted for RANKED: assignmentId={}", assignment.getId());
      return false;
    }

    AssignmentOffer offer = assignment.createOffer(next.get().getAgentId(), offerExpiresAt());

    if (assignment.getStatus() == com.ebikes.assignments.enums.AssignmentStatus.STARTED) {
      assignment.awaitResponse();
    }

    assignmentRepository.save(assignment);

    outboxService.save(
        EventTypes.AssignmentOffers.CREATED,
        new OfferCreatedEvent(
            offer.getAgentId(), assignment.getId(), offer.getExpiresAt(), offer.getId()),
        RoutingKeys.ASSIGNMENT_OFFER_CREATED);

    log.info(
        "RANKED offer created: assignmentId={}, agentId={}",
        assignment.getId(),
        next.get().getAgentId());

    return true;
  }

  @Override
  public AssignmentStrategy supports() {
    return AssignmentStrategy.RANKED;
  }

  private void assignRanksFromMatrix(
      Assignment assignment, OrderContext orderContext, List<ShortlistCandidate> candidates) {

    MatrixResponseDto matrix = assignmentsServiceClient.computeMatrix(orderContext, candidates);

    Map<String, Integer> durationByAgentId =
        matrix.entries().stream()
            .collect(Collectors.toMap(MatrixEntryDto::agentId, MatrixEntryDto::durationSeconds));

    List<String> rankedAgentIds =
        candidates.stream()
            .sorted(
                Comparator.comparingInt(
                    c -> durationByAgentId.getOrDefault(c.getAgentId(), Integer.MAX_VALUE)))
            .map(ShortlistCandidate::getAgentId)
            .toList();

    assignment.rankCandidates(rankedAgentIds);
    assignmentRepository.save(assignment);

    log.debug(
        "Matrix ranks assigned: assignmentId={}, candidateCount={}",
        assignment.getId(),
        rankedAgentIds.size());
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
