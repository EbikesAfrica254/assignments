package com.ebikes.assignments.services.assignments.strategies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.mappers.OfferMapper;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.services.offers.OfferExpiryService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.services.shortlist.RankingService;
import com.ebikes.assignments.services.shortlist.ShortlistService;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OfferFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.ShortlistFixtures;

@DisplayName("RankedStrategy")
@ExtendWith(MockitoExtension.class)
class RankedStrategyTest {

  @Mock private OfferExpiryService offerExpiryService;
  @Mock private OfferMapper offerMapper;
  @Mock private OfferService offerService;
  @Mock private OutboxService outboxService;
  @Mock private RankingService rankingService;
  @Mock private ShortlistService shortlistService;

  private RankedStrategy strategy;

  private Assignment assignment;
  private OrderContext orderContext;

  @BeforeEach
  void setUp() {
    strategy =
        new RankedStrategy(
            offerMapper,
            offerService,
            offerExpiryService,
            outboxService,
            rankingService,
            shortlistService);

    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.RANKED);
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should rank candidates when not yet ranked")
    void shouldRankCandidatesWhenNotYetRanked() {
      when(rankingService.isRanked(assignment.getId())).thenReturn(false);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of())).thenReturn(List.of());

      strategy.execute(assignment, orderContext);

      verify(rankingService).rank(same(assignment), same(orderContext));
    }

    @Test
    @DisplayName("should skip ranking when already ranked")
    void shouldSkipRankingWhenAlreadyRanked() {
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of())).thenReturn(List.of());

      strategy.execute(assignment, orderContext);

      verify(rankingService, never()).rank(any(), any());
    }

    @Test
    @DisplayName("should return false when no eligible candidates exist")
    void shouldReturnFalseWhenNoEligibleCandidatesExist() {
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of())).thenReturn(List.of());

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should not publish when no eligible candidates exist")
    void shouldNotPublishWhenNoEligibleCandidatesExist() {
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of())).thenReturn(List.of());

      strategy.execute(assignment, orderContext);

      verify(outboxService, never()).publish(any(), any(), any());
    }

    @Test
    @DisplayName("should return true when eligible candidate exists")
    void shouldReturnTrueWhenEligibleCandidateExists() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.ranked(assignment, 1);
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of()))
          .thenReturn(List.of(entry));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(entry.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should create offer for highest ranked eligible candidate")
    void shouldCreateOfferForHighestRankedEligibleCandidate() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.ranked(assignment, 1);
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of()))
          .thenReturn(List.of(entry));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(entry.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      verify(offerService).create(eq(entry.getAgentId()), same(assignment), eq(expiresAt));
    }

    @Test
    @DisplayName("should publish offer created event with correct routing key")
    void shouldPublishOfferCreatedEventWithCorrectRoutingKey() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.ranked(assignment, 1);
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of()))
          .thenReturn(List.of(entry));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(entry.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      verify(outboxService).publish(any(), any(), eq(RoutingKeys.ASSIGNMENT_OFFER_CREATED));
    }

    @Test
    @DisplayName("should transition assignment to awaiting response")
    void shouldTransitionAssignmentToAwaitingResponse() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.ranked(assignment, 1);
      when(rankingService.isRanked(assignment.getId())).thenReturn(true);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findRankedEligible(assignment.getId(), Set.of()))
          .thenReturn(List.of(entry));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(entry.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.AWAITING_RESPONSE);
    }
  }

  @Nested
  @DisplayName("supports")
  class Supports {

    @Test
    @DisplayName("should support ranked strategy")
    void shouldSupportRankedStrategy() {
      assertThat(strategy.supports()).isEqualTo(AssignmentStrategy.RANKED);
    }
  }
}
