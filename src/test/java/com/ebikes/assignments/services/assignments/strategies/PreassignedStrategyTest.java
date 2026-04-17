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
import java.util.Optional;
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
import com.ebikes.assignments.services.shortlist.ShortlistService;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OfferFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.ShortlistFixtures;

@DisplayName("PreassignedStrategy")
@ExtendWith(MockitoExtension.class)
class PreassignedStrategyTest {

  @Mock private OfferExpiryService offerExpiryService;
  @Mock private OfferMapper offerMapper;
  @Mock private OfferService offerService;
  @Mock private OutboxService outboxService;
  @Mock private ShortlistService shortlistService;

  private PreassignedStrategy strategy;

  private Assignment assignment;
  private OrderContext orderContext;

  @BeforeEach
  void setUp() {
    strategy =
        new PreassignedStrategy(
            offerMapper, offerService, offerExpiryService, outboxService, shortlistService);

    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.PREASSIGNED);
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should return false when no preferred candidate exists")
    void shouldReturnFalseWhenNoPreferredCandidateExists() {
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.empty());

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should not create offer when no preferred candidate exists")
    void shouldNotCreateOfferWhenNoPreferredCandidateExists() {
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.empty());

      strategy.execute(assignment, orderContext);

      verify(offerService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("should return false when preferred candidate is exhausted")
    void shouldReturnFalseWhenPreferredCandidateIsExhausted() {
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.of(preferred));
      when(offerService.getExhaustedAgentIds(assignment.getId()))
          .thenReturn(Set.of(preferred.getAgentId()));

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isFalse();
    }

    @Test
    @DisplayName("should not create offer when preferred candidate is exhausted")
    void shouldNotCreateOfferWhenPreferredCandidateIsExhausted() {
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.of(preferred));
      when(offerService.getExhaustedAgentIds(assignment.getId()))
          .thenReturn(Set.of(preferred.getAgentId()));

      strategy.execute(assignment, orderContext);

      verify(offerService, never()).create(any(), any(), any());
    }

    @Test
    @DisplayName("should return true when preferred candidate exists and is not exhausted")
    void shouldReturnTrueWhenPreferredCandidateExists() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.of(preferred));
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(preferred.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      boolean result = strategy.execute(assignment, orderContext);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("should create offer for preferred candidate")
    void shouldCreateOfferForPreferredCandidate() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.of(preferred));
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(preferred.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      verify(offerService).create(eq(preferred.getAgentId()), same(assignment), eq(expiresAt));
    }

    @Test
    @DisplayName("should publish offer created event with correct routing key")
    void shouldPublishOfferCreatedEventWithCorrectRoutingKey() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.of(preferred));
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(preferred.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      verify(outboxService).publish(any(), any(), eq(RoutingKeys.ASSIGNMENT_OFFER_CREATED));
    }

    @Test
    @DisplayName("should transition assignment to awaiting response")
    void shouldTransitionAssignmentToAwaitingResponse() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      Shortlist preferred = ShortlistFixtures.preferred(assignment);
      when(shortlistService.findPreferred(assignment.getId())).thenReturn(Optional.of(preferred));
      when(offerService.getExhaustedAgentIds(assignment.getId())).thenReturn(Set.of());
      when(offerExpiryService.offerExpiresAt()).thenReturn(expiresAt);
      when(offerService.create(eq(preferred.getAgentId()), same(assignment), eq(expiresAt)))
          .thenReturn(OfferFixtures.created(assignment));

      strategy.execute(assignment, orderContext);

      assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.AWAITING_RESPONSE);
    }
  }

  @Nested
  @DisplayName("supports")
  class Supports {

    @Test
    @DisplayName("should support preassigned strategy")
    void shouldSupportPreassignedStrategy() {
      assertThat(strategy.supports()).isEqualTo(AssignmentStrategy.PREASSIGNED);
    }
  }
}
