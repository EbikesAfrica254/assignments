package com.ebikes.assignments.services.shortlist;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.adapters.routing.RoutingServiceAdapter;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.database.repositories.ShortlistRepository;
import com.ebikes.assignments.dtos.internal.MatrixEntry;
import com.ebikes.assignments.dtos.internal.MatrixResponse;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.BusinessRuleException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class RankingService {

  private final RoutingServiceAdapter routingServiceAdapter;
  private final ShortlistRepository shortlistRepository;

  @Transactional
  public void rank(Assignment assignment, OrderContext orderContext) {
    assignment.statusNotTerminal();

    List<Shortlist> candidates =
        shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId());

    if (candidates.isEmpty()) {
      throw new BusinessRuleException(
          ResponseCode.RESOURCE_NOT_FOUND,
          "No shortlist candidates found for assignmentId: " + assignment.getId());
    }

    List<String> rankedAgentIds = compute(orderContext, candidates);
    apply(assignment, rankedAgentIds);

    log.info("Candidates ranked: assignmentId={}, count={}", assignment.getId(), candidates.size());
  }

  @Transactional(readOnly = true)
  public boolean isRanked(UUID assignmentId) {
    return shortlistRepository.findByAssignmentIdOrderByRankAsc(assignmentId).stream()
        .anyMatch(s -> s.getRank() != null);
  }

  private List<String> compute(OrderContext orderContext, List<Shortlist> candidates) {
    MatrixResponse matrix = routingServiceAdapter.computeMatrix(orderContext, candidates);

    return matrix.entries().stream()
        .sorted(Comparator.comparingInt(MatrixEntry::durationSeconds))
        .map(MatrixEntry::agentId)
        .toList();
  }

  private void apply(Assignment assignment, List<String> agentIdsInRankOrder) {
    if (agentIdsInRankOrder == null || agentIdsInRankOrder.isEmpty()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_ARGUMENTS, "agentIdsInRankOrder must not be null or empty");
    }

    List<Shortlist> entries =
        shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId());

    for (int i = 0; i < agentIdsInRankOrder.size(); i++) {
      String agentId = agentIdsInRankOrder.get(i);
      entries.stream()
          .filter(e -> e.getAgentId().equals(agentId))
          .findFirst()
          .orElseThrow(
              () ->
                  new BusinessRuleException(
                      ResponseCode.RESOURCE_NOT_FOUND,
                      "Shortlist entry not found for agentId: " + agentId))
          .assignRank(i + 1);
    }

    shortlistRepository.saveAll(entries);
  }
}
