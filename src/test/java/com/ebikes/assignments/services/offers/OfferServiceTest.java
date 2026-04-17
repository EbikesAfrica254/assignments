package com.ebikes.assignments.services.offers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.ebikes.assignments.constants.EventConstants.DomainEvents;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Offer;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.repositories.OfferRepository;
import com.ebikes.assignments.dtos.events.internal.OfferDeclinedTrigger;
import com.ebikes.assignments.dtos.responses.offers.OfferResponse;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.exceptions.BusinessRuleException;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.OfferMapper;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.audit.AuditTemplate;
import com.ebikes.assignments.support.audit.ThrowingRunnable;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OfferFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;

@DisplayName("OfferService")
@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private AuditTemplate auditTemplate;
  @Mock private OfferMapper offerMapper;
  @Mock private OfferRepository offerRepository;
  @Mock private OutboxService outboxService;

  private OfferService service;
  private Assignment assignment;

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(3);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(Offer.class), anyString(), anyString(), any(ThrowingRunnable.class));
  }

  @BeforeEach
  void setUp() {
    service =
        new OfferService(
            applicationEventPublisher, auditTemplate, offerMapper, offerRepository, outboxService);

    OrderContext orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.awaitingResponse(orderContext);
  }

  @Nested
  @DisplayName("accept")
  class Accept {

    @Test
    @DisplayName("should accept offer and persist")
    void shouldAcceptOfferAndPersist() {
      wireAuditTemplate();
      Offer offer = OfferFixtures.created(assignment);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

      service.accept(offer.getId(), offer.getAgentId());

      assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
      verify(offerRepository).save(offer);
    }

    @Test
    @DisplayName("should publish accepted event to outbox")
    void shouldPublishAcceptedEventToOutbox() {
      Offer offer = OfferFixtures.created(assignment);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

      service.accept(offer.getId(), offer.getAgentId());

      verify(outboxService)
          .publish(
              eq(DomainEvents.Offers.ACCEPTED), any(), eq(RoutingKeys.ASSIGNMENT_OFFER_ACCEPTED));
    }

    @Test
    @DisplayName("should return mapped response")
    void shouldReturnMappedResponse() {
      Offer offer = OfferFixtures.created(assignment);
      OfferResponse response =
          new OfferResponse(
              offer.getId(),
              offer.getAgentId(),
              UUID.randomUUID(),
              null,
              offer.getExpiresAt(),
              null,
              OfferStatus.ACCEPTED);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
      when(offerMapper.toResponse(offer)).thenReturn(response);

      OfferResponse result = service.accept(offer.getId(), offer.getAgentId());

      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when offer absent")
    void shouldThrowResourceNotFoundWhenOfferAbsent() {
      UUID offerId = UUID.randomUUID();
      when(offerRepository.findById(offerId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.accept(offerId, "agent-001"))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent does not own offer")
    void shouldThrowBusinessRuleWhenAgentDoesNotOwnOffer() {
      Offer offer = OfferFixtures.created(assignment);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

      String agentId = "agent-999";
      UUID offerId = offer.getId();
      assertThatThrownBy(() -> service.accept(offerId, agentId))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  @DisplayName("decline")
  class Decline {

    @Test
    @DisplayName("should decline offer and persist")
    void shouldDeclineOfferAndPersist() {
      wireAuditTemplate();
      Offer offer = OfferFixtures.created(assignment);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

      service.decline(offer.getId(), offer.getAgentId());

      assertThat(offer.getStatus()).isEqualTo(OfferStatus.DECLINED);
      verify(offerRepository).save(offer);
    }

    @Test
    @DisplayName("should publish declined event to outbox")
    void shouldPublishDeclinedEventToOutbox() {
      Offer offer = OfferFixtures.created(assignment);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

      service.decline(offer.getId(), offer.getAgentId());

      verify(outboxService)
          .publish(
              eq(DomainEvents.Offers.DECLINED), any(), eq(RoutingKeys.ASSIGNMENT_OFFER_DECLINED));
    }

    @Test
    @DisplayName("should publish offer declined event")
    void shouldPublishOfferDeclined() {
      Offer offer = OfferFixtures.created(assignment);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(UUID.randomUUID(), AssignmentStrategy.PREASSIGNED);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
      when(offerMapper.toDeclinedTrigger(offer)).thenReturn(trigger);

      service.decline(offer.getId(), offer.getAgentId());

      verify(applicationEventPublisher).publishEvent(trigger);
    }

    @Test
    @DisplayName("should return mapped response")
    void shouldReturnMappedResponse() {
      Offer offer = OfferFixtures.created(assignment);
      OfferResponse response =
          new OfferResponse(
              offer.getId(),
              offer.getAgentId(),
              UUID.randomUUID(),
              null,
              offer.getExpiresAt(),
              null,
              OfferStatus.DECLINED);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));
      when(offerMapper.toResponse(offer)).thenReturn(response);

      OfferResponse result = service.decline(offer.getId(), offer.getAgentId());

      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when offer absent")
    void shouldThrowResourceNotFoundWhenOfferAbsent() {
      UUID offerId = UUID.randomUUID();
      when(offerRepository.findById(offerId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.decline(offerId, "agent-001"))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent does not own offer")
    void shouldThrowBusinessRuleWhenAgentDoesNotOwnOffer() {
      Offer offer = OfferFixtures.created(assignment);
      when(offerRepository.findById(offer.getId())).thenReturn(Optional.of(offer));

      String agentId = "agent-999";
      UUID offerId = offer.getId();
      assertThatThrownBy(() -> service.decline(offerId, agentId))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should persist and return offer when agent is not exhausted")
    void shouldPersistAndReturnOffer() {
      Offer offer = OfferFixtures.created(assignment);
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      when(offerRepository.findExhaustedAgentIds(eq(assignment.getId()), any()))
          .thenReturn(Set.of());
      when(offerRepository.save(any(Offer.class))).thenReturn(offer);

      Offer result = service.create("agent-001", assignment, expiresAt);

      assertThat(result).isEqualTo(offer);
      verify(offerRepository).save(any(Offer.class));
    }

    @Test
    @DisplayName("should throw BusinessRuleException when agent is exhausted")
    void shouldThrowBusinessRuleWhenAgentIsExhausted() {
      OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30);
      when(offerRepository.findExhaustedAgentIds(eq(assignment.getId()), any()))
          .thenReturn(Set.of("agent-001"));

      assertThatThrownBy(() -> service.create("agent-001", assignment, expiresAt))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  @DisplayName("cancelActiveOffers")
  class CancelActiveOffers {

    @Test
    @DisplayName("should cancel non-terminal offers and save")
    void shouldCancelNonTerminalOffersAndSave() {
      Offer first = OfferFixtures.created(assignment);
      Offer second = OfferFixtures.created(assignment);
      when(offerRepository.findByAssignmentId(assignment.getId()))
          .thenReturn(List.of(first, second));

      service.cancelActiveOffers(assignment);

      assertThat(first.getStatus()).isEqualTo(OfferStatus.CANCELLED);
      assertThat(second.getStatus()).isEqualTo(OfferStatus.CANCELLED);
      verify(offerRepository).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("should publish cancelled event per non-terminal offer")
    void shouldPublishCancelledEventPerOffer() {
      Offer first = OfferFixtures.created(assignment);
      Offer second = OfferFixtures.created(assignment);
      when(offerRepository.findByAssignmentId(assignment.getId()))
          .thenReturn(List.of(first, second));

      service.cancelActiveOffers(assignment);

      verify(outboxService, times(2))
          .publish(
              eq(DomainEvents.Offers.CANCELLED), any(), eq(RoutingKeys.ASSIGNMENT_OFFER_CANCELLED));
    }

    @Test
    @DisplayName("should skip terminal offers")
    void shouldSkipTerminalOffers() {
      Offer active = OfferFixtures.created(assignment);
      Offer terminal = OfferFixtures.accepted(assignment);
      when(offerRepository.findByAssignmentId(assignment.getId()))
          .thenReturn(List.of(active, terminal));

      service.cancelActiveOffers(assignment);

      assertThat(active.getStatus()).isEqualTo(OfferStatus.CANCELLED);
      assertThat(terminal.getStatus()).isEqualTo(OfferStatus.ACCEPTED);

      ArgumentCaptor<List<Offer>> captor = ArgumentCaptor.captor();
      verify(offerRepository).saveAll(captor.capture());
      assertThat(captor.getValue()).containsExactly(active);
    }

    @Test
    @DisplayName("should do nothing when no offers exist")
    void shouldDoNothingWhenNoOffersExist() {
      when(offerRepository.findByAssignmentId(assignment.getId())).thenReturn(List.of());

      service.cancelActiveOffers(assignment);

      verify(offerRepository).saveAll(List.of());
      verify(outboxService, never()).publish(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("hasLiveOffer")
  class HasLiveOffer {

    @Test
    @DisplayName("should return true when a CREATED offer exists")
    void shouldReturnTrueWhenCreatedOfferExists() {
      when(offerRepository.existsByAssignmentIdAndStatus(assignment.getId(), OfferStatus.CREATED))
          .thenReturn(true);

      assertThat(service.hasLiveOffer(assignment.getId())).isTrue();
    }

    @Test
    @DisplayName("should return false when no CREATED offer exists")
    void shouldReturnFalseWhenNoCreatedOfferExists() {
      when(offerRepository.existsByAssignmentIdAndStatus(assignment.getId(), OfferStatus.CREATED))
          .thenReturn(false);

      assertThat(service.hasLiveOffer(assignment.getId())).isFalse();
    }
  }

  @Nested
  @DisplayName("getExhaustedAgentIds")
  class GetExhaustedAgentIds {

    @Test
    @DisplayName("should delegate to repository with all exhausted statuses")
    void shouldDelegateToRepositoryWithAllExhaustedStatuses() {
      Set<String> exhausted = Set.of("agent-001", "agent-002");
      ArgumentCaptor<Set<OfferStatus>> statusCaptor = ArgumentCaptor.captor();
      when(offerRepository.findExhaustedAgentIds(eq(assignment.getId()), statusCaptor.capture()))
          .thenReturn(exhausted);

      Set<String> result = service.getExhaustedAgentIds(assignment.getId());

      assertThat(result).isEqualTo(exhausted);
      assertThat(statusCaptor.getValue())
          .containsExactlyInAnyOrder(
              OfferStatus.ACCEPTED,
              OfferStatus.DECLINED,
              OfferStatus.EXPIRED,
              OfferStatus.EXPIRY_FAILED);
    }
  }
}
