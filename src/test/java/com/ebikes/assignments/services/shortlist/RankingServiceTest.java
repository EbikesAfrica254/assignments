package com.ebikes.assignments.services.shortlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.adapters.routing.RoutingServiceAdapter;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.database.repositories.ShortlistRepository;
import com.ebikes.assignments.dtos.internal.MatrixEntry;
import com.ebikes.assignments.dtos.internal.MatrixResponse;
import com.ebikes.assignments.exceptions.BusinessRuleException;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.ShortlistFixtures;

@DisplayName("RankingService")
@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

  @Mock private RoutingServiceAdapter routingServiceAdapter;
  @Mock private ShortlistRepository shortlistRepository;

  @InjectMocks private RankingService service;

  private Assignment assignment;
  private OrderContext orderContext;

  @BeforeEach
  void setUp() {
    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.awaitingResponse(orderContext);
  }

  @Nested
  @DisplayName("rank")
  class Rank {

    @Test
    @DisplayName("should throw BusinessRuleException when assignment is in a terminal state")
    void shouldThrowWhenAssignmentIsTerminal() {
      Assignment cancelled = AssignmentFixtures.cancelled(orderContext);

      assertThatThrownBy(() -> service.rank(cancelled, orderContext))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when no shortlist candidates found")
    void shouldThrowWhenNoCandidatesFound() {
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of());

      assertThatThrownBy(() -> service.rank(assignment, orderContext))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should rank candidates ordered by duration ascending")
    void shouldRankCandidatesOrderedByDuration() {
      Shortlist slower = ShortlistFixtures.unranked(assignment);
      Shortlist faster = ShortlistFixtures.unranked(assignment);

      MatrixResponse matrix =
          new MatrixResponse(
              List.of(
                  new MatrixEntry(slower.getAgentId(), 1000, 300, false),
                  new MatrixEntry(faster.getAgentId(), 500, 120, false)),
              BigDecimal.ZERO,
              BigDecimal.ZERO);

      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(slower, faster));
      when(routingServiceAdapter.computeMatrix(orderContext, List.of(slower, faster)))
          .thenReturn(matrix);

      service.rank(assignment, orderContext);

      assertThat(faster.getRank()).isEqualTo(1);
      assertThat(slower.getRank()).isEqualTo(2);
    }

    @Test
    @DisplayName("should save all shortlist entries after ranking")
    void shouldSaveAllEntriesAfterRanking() {
      Shortlist first = ShortlistFixtures.unranked(assignment);
      Shortlist second = ShortlistFixtures.unranked(assignment);

      MatrixResponse matrix =
          new MatrixResponse(
              List.of(
                  new MatrixEntry(first.getAgentId(), 800, 200, false),
                  new MatrixEntry(second.getAgentId(), 400, 100, false)),
              BigDecimal.ZERO,
              BigDecimal.ZERO);

      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(first, second));
      when(routingServiceAdapter.computeMatrix(orderContext, List.of(first, second)))
          .thenReturn(matrix);

      service.rank(assignment, orderContext);

      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<Shortlist>> captor = ArgumentCaptor.forClass(List.class);
      verify(shortlistRepository).saveAll(captor.capture());
      assertThat(captor.getValue()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    @DisplayName("should not assign rank to agent absent from matrix response")
    void shouldNotAssignRankToAgentAbsentFromMatrix() {
      Shortlist present = ShortlistFixtures.unranked(assignment);
      Shortlist absent = ShortlistFixtures.unranked(assignment);

      MatrixResponse matrix =
          new MatrixResponse(
              List.of(new MatrixEntry(present.getAgentId(), 500, 150, false)),
              BigDecimal.ZERO,
              BigDecimal.ZERO);

      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(present, absent));
      when(routingServiceAdapter.computeMatrix(orderContext, List.of(present, absent)))
          .thenReturn(matrix);

      service.rank(assignment, orderContext);

      assertThat(present.getRank()).isEqualTo(1);
      assertThat(absent.getRank()).isNull();
    }
  }

  @Nested
  @DisplayName("isRanked")
  class IsRanked {

    @Test
    @DisplayName("should return true when at least one shortlist entry has a rank")
    void shouldReturnTrueWhenAtLeastOneEntryHasRank() {
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(ShortlistFixtures.ranked(assignment, 1)));

      assertThat(service.isRanked(assignment.getId())).isTrue();
    }

    @Test
    @DisplayName("should return false when no shortlist entries have a rank")
    void shouldReturnFalseWhenNoEntriesHaveRank() {
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of(ShortlistFixtures.unranked(assignment)));

      assertThat(service.isRanked(assignment.getId())).isFalse();
    }

    @Test
    @DisplayName("should return false when shortlist is empty")
    void shouldReturnFalseWhenShortlistIsEmpty() {
      when(shortlistRepository.findByAssignmentIdOrderByRankAsc(assignment.getId()))
          .thenReturn(List.of());

      assertThat(service.isRanked(assignment.getId())).isFalse();
    }
  }
}
