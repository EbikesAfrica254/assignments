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

import com.ebikes.assignments.configurations.properties.ServiceProperties;
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
import com.ebikes.assignments.services.shortlist.ShortlistService;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OfferFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.ShortlistFixtures;

@DisplayName("BroadcastStrategy")
@ExtendWith(MockitoExtension.class)
class BroadcastStrategyTest {

  @Mock private OfferExpiryService offerExpiryService;
  @Mock private OfferMapper offerMapper;
  @Mock private OfferService offerService;
  @Mock private OutboxService outboxService;
  @Mock private ServiceProperties serviceProperties;
  @Mock private ShortlistService shortlistService;

  private BroadcastStrategy strategy;

  private Assignment assignment;
  private OrderContext orderContext;

  @BeforeEach
  void setUp() {
    strategy =
        new BroadcastStrategy(
            serviceProperties,
            offerMapper,
            offerService,
            offerExpiryService,
            outboxService,
            shortlistService);

    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.BROADCAST);
  }

  private void stubBroadcastMax() {
    ServiceProperties.Candidates candidateProps = new ServiceProperties.Candidates();
    candidateProps.setBroadcastMax(5);
    when(serviceProperties.getCandidates()).thenReturn(candidateProps);
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should return false when no eligible candidates exist")
    void shouldReturnFalseWhenNoEligibleCandidatesExist() {
      stubBroadcastMax();
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findEligible(assignment.getId(), Set.of(), 5)).thenReturn(List.of());

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should not publish when no eligible candidates exist")
    void shouldNotPublishWhenNoEligibleCandidatesExist() {
      stubBroadcastMax();
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findEligible(assignment.getId(), Set.of(), 5)).thenReturn(List.of());

      strategy.execute(assignment, orderContext);

      verify(outboxService, never()).bulkPublish(any(), any(), any());
    }

    @Test
    @DisplayName("should return true when eligible candidates exist")
    void shouldReturnTrueWhenEligibleCandidatesExist() {
      stubBroadcastMax();
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.unranked(assignment);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findEligible(assignment.getId(), Set.of(), 5))
          .thenReturn(List.of(entry));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(entry.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should create one offer per eligible candidate")
    void shouldCreateOneOfferPerEligibleCandidate() {
      stubBroadcastMax();
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist first = ShortlistFixtures.unranked(assignment);
      Shortlist second = ShortlistFixtures.unranked(assignment);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findEligible(assignment.getId(), Set.of(), 5))
          .thenReturn(List.of(first, second));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(first.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));
      when(offerService.create(eq(second.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      verify(offerService).create(eq(first.getAgentId()), same(assignment), eq(expiresAt));
      verify(offerService).create(eq(second.getAgentId()), same(assignment), eq(expiresAt));
    }

    @Test
    @DisplayName("should publish bulk offers with correct routing key")
    void shouldPublishBulkOffersWithCorrectRoutingKey() {
      stubBroadcastMax();
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.unranked(assignment);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findEligible(assignment.getId(), Set.of(), 5))
          .thenReturn(List.of(entry));
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(entry.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      verify(outboxService).bulkPublish(any(), any(), eq(RoutingKeys.ASSIGNMENT_OFFER_CREATED));
    }

    @Test
    @DisplayName("should transition assignment to awaiting response")
    void shouldTransitionAssignmentToAwaitingResponse() {
      stubBroadcastMax();
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist entry = ShortlistFixtures.unranked(assignment);
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(shortlistService.findEligible(assignment.getId(), Set.of(), 5))
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
    @DisplayName("should support broadcast strategy")
    void shouldSupportBroadcastStrategy() {
      assertThat(strategy.supports()).isEqualTo(AssignmentStrategy.BROADCAST);
    }
  }
}
