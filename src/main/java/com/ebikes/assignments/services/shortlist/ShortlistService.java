package com.ebikes.assignments.services.shortlist;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.database.repositories.ShortlistRepository;
import com.ebikes.assignments.dtos.internal.Candidate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class ShortlistService {

  private final ShortlistRepository shortlistRepository;

  @Transactional
  public void create(Assignment assignment, List<Candidate> candidates) {
    assignment.statusNotTerminal();

    List<Shortlist> entries = candidates.stream().map(c -> build(c, assignment)).toList();

    shortlistRepository.saveAll(entries);

    log.info("Shortlist persisted: assignmentId={}, count={}", assignment.getId(), entries.size());
  }

  @Transactional(readOnly = true)
  public List<Shortlist> findEligible(UUID assignmentId, Set<String> exhaustedAgentIds, int limit) {
    return shortlistRepository.findByAssignmentIdOrderByRankAsc(assignmentId).stream()
        .filter(s -> !exhaustedAgentIds.contains(s.getAgentId()))
        .limit(limit)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Shortlist> findRankedEligible(UUID assignmentId, Set<String> exhaustedAgentIds) {
    return shortlistRepository
        .findByAssignmentIdAndRankIsNotNullOrderByRankAsc(assignmentId)
        .stream()
        .filter(s -> !exhaustedAgentIds.contains(s.getAgentId()))
        .limit(1)
        .toList();
  }

  @Transactional(readOnly = true)
  public Optional<Shortlist> findPreferred(UUID assignmentId) {
    return shortlistRepository.findByAssignmentIdOrderByRankAsc(assignmentId).stream()
        .filter(Shortlist::isPreferred)
        .findFirst();
  }

  private Shortlist build(Candidate candidate, Assignment assignment) {
    return Shortlist.builder()
        .agentId(candidate.agentId())
        .assignment(assignment)
        .isPreferred(candidate.isPreferred())
        .latitude(candidate.latitude())
        .longitude(candidate.longitude())
        .vehicleClass(candidate.vehicleClass())
        .build();
  }
}
