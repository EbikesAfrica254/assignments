package com.ebikes.assignments.services.shortlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.database.repositories.ShortlistRepository;
import com.ebikes.assignments.dtos.internal.Candidate;
import com.ebikes.assignments.exceptions.BusinessRuleException;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.CandidateFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.ShortlistFixtures;

@DisplayName("ShortlistService")
@ExtendWith(MockitoExtension.class)
class ShortlistServiceTest {

  @Mock private ShortlistRepository shortlistRepository;

  @InjectMocks private ShortlistService service;

  private Assignment assignment;
  private OrderContext orderContext;

  @BeforeEach
  void setUp() {
    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.awaitingResponse(orderContext);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should throw BusinessRuleException when assignment is in a terminal state")
    void shouldThrowWhenAssignmentIsTerminal() {
      Assignment cancelled = AssignmentFixtures.cancelled(orderContext);

      List<Candidate> candidates = List.of(CandidateFixtures.standard());
      assertThatThrownBy(() -> service.create(cancelled, candidates))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should persist all candidates as shortlist entries")
    void shouldPersistAllCandidatesAsShortlistEntries() {
      Candidate first = CandidateFixtures.standard();
      Candidate second = CandidateFixtures.standard();

      service.create(assignment, List.of(first, second));

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<Shortlist>> captor = ArgumentCaptor.forClass(List.class);
      verify(shortlistRepository).saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("should map candidate fields to shortlist entry")
    void shouldMapCandidateFieldsToShortlistEntry() {
      Candidate candidate = CandidateFixtures.preferred();

      service.create(assignment, List.of(candidate));

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<Shortlist>> captor = ArgumentCaptor.forClass(List.class);
      verify(shortlistRepository).saveAll(captor.capture());
      Shortlist saved = captor.getValue().getFirst();
      assertThat(saved.getAgentId()).isEqualTo(candidate.agentId());
      assertThat(saved.isPreferred()).isEqualTo(candidate.isPreferred());
      assertThat(saved.getLatitude()).isEqualByComparingTo(candidate.latitude());
      assertThat(saved.getLongitude()).isEqualByComparingTo(candidate.longitude());
      assertThat(saved.getVehicleClass()).isEqualTo(candidate.vehicleClass());
    }
  }

  @Nested
  @DisplayName("findEligible")
  class FindEligible {

    @Test
    @DisplayName("should return candidates not in exhausted agent id set")
    void shouldReturnCandidatesNotInExhaustedSet() {
      Shortlist eligible = ShortlistFixtures.unranked(assignment);
      Shortlist exhausted = ShortlistFixtures.unranked(assignment);
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(eligible, exhausted));

      List<Shortlist> result =
          service.findEligible(assignment.getId(), Set.of(exhausted.getAgentId()), 10);

      assertThat(result).containsExactly(eligible);
    }

    @Test
    @DisplayName("should return empty list when all candidates are exhausted")
    void shouldReturnEmptyListWhenAllCandidatesExhausted() {
      Shortlist first = ShortlistFixtures.unranked(assignment);
      Shortlist second = ShortlistFixtures.unranked(assignment);
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(first, second));

      List<Shortlist> result =
          service.findEligible(
              assignment.getId(), Set.of(first.getAgentId(), second.getAgentId()), 10);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should respect the limit parameter")
    void shouldRespectLimit() {
      Shortlist first = ShortlistFixtures.unranked(assignment);
      Shortlist second = ShortlistFixtures.unranked(assignment);
      Shortlist third = ShortlistFixtures.unranked(assignment);
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(first, second, third));

      List<Shortlist> result = service.findEligible(assignment.getId(), Set.of(), 2);

      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("findPreferred")
  class FindPreferred {

    @Test
    @DisplayName("should return the preferred shortlist entry")
    void shouldReturnPreferredEntry() {
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      Shortlist unranked = ShortlistFixtures.unranked(assignment);
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(preferred, unranked));

      Optional<Shortlist> result = service.findPreferred(assignment.getId());

      assertThat(result).contains(preferred);
    }

    @Test
    @DisplayName("should return empty when no preferred shortlist entry exists")
    void shouldReturnEmptyWhenNoPreferredEntry() {
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(ShortlistFixtures.unranked(assignment)));

      assertThat(service.findPreferred(assignment.getId())).isEmpty();
    }

    @Test
    @DisplayName("should return empty when shortlist is empty")
    void shouldReturnEmptyWhenShortlistIsEmpty() {
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of());

      assertThat(service.findPreferred(assignment.getId())).isEmpty();
    }
  }
}
