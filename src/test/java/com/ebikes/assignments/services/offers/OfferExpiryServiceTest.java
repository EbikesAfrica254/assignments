package com.ebikes.assignments.services.offers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.ebikes.assignments.configurations.properties.ServiceProperties;
import com.ebikes.assignments.database.projections.AssignmentReference;
import com.ebikes.assignments.database.repositories.OfferRepository;
import com.ebikes.assignments.dtos.events.internal.OfferExpiredTrigger;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.mappers.OfferMapper;
import com.ebikes.assignments.support.context.ExecutionContext;

@DisplayName("OfferExpiryService")
@ExtendWith(MockitoExtension.class)
class OfferExpiryServiceTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private OfferMapper offerMapper;
  @Mock private OfferRepository offerRepository;
  @Mock private ServiceProperties serviceProperties;

  @InjectMocks private OfferExpiryService service;

  @Nested
  @DisplayName("offerExpiresAt")
  class OfferExpiresAt {

    @Test
    @DisplayName("should return a future time offset by the configured expiry minutes")
    void shouldReturnFutureTimeBasedOnExpiryMinutes() {
      ServiceProperties.Offers offerProps = new ServiceProperties.Offers();
      offerProps.setExpiryMinutes(30);
      when(serviceProperties.getOffers()).thenReturn(offerProps);

      OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
      OffsetDateTime result = service.offerExpiresAt();
      OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

      assertThat(result).isAfter(before.plusMinutes(29)).isBefore(after.plusMinutes(31));
    }
  }

  @Nested
  @DisplayName("processExpired")
  class ProcessExpired {

    @BeforeEach
    void setUp() {
      ExecutionContext.setSystem();
    }

    @Test
    @DisplayName("should do nothing when no expired offers are found")
    void shouldDoNothingWhenNoExpiredOffersFound() {
      when(offerRepository.findIdsByStatusAndExpiresAtBefore(
              eq(OfferStatus.CREATED), any(OffsetDateTime.class)))
          .thenReturn(List.of());

      service.processExpired();

      verify(offerRepository, never()).bulkUpdateStatus(any(), any());
    }

    @Test
    @DisplayName("should bulk update expired offer status to EXPIRED")
    void shouldBulkUpdateExpiredOfferStatus() {
      UUID firstId = UUID.randomUUID();
      UUID secondId = UUID.randomUUID();
      List<UUID> expiredIds = List.of(firstId, secondId);
      when(offerRepository.findIdsByStatusAndExpiresAtBefore(
              eq(OfferStatus.CREATED), any(OffsetDateTime.class)))
          .thenReturn(expiredIds);
      when(offerRepository.findAssignmentReferencesByIds(expiredIds)).thenReturn(List.of());

      service.processExpired();

      verify(offerRepository).bulkUpdateStatus(expiredIds, OfferStatus.EXPIRED);
    }

    @Test
    @DisplayName("should publish one progress trigger event per assignment reference")
    void shouldPublishOneEventPerAssignmentReference() {
      UUID firstId = UUID.randomUUID();
      UUID secondId = UUID.randomUUID();
      List<UUID> expiredIds = List.of(firstId, secondId);

      AssignmentReference firstReference =
          new AssignmentReference(UUID.randomUUID(), AssignmentStrategy.BROADCAST);
      AssignmentReference secondReference =
          new AssignmentReference(UUID.randomUUID(), AssignmentStrategy.RANKED);
      OfferExpiredTrigger firstTrigger =
          new OfferExpiredTrigger(firstReference.assignmentId(), firstReference.strategy());
      OfferExpiredTrigger secondTrigger =
          new OfferExpiredTrigger(secondReference.assignmentId(), secondReference.strategy());

      when(offerRepository.findIdsByStatusAndExpiresAtBefore(
              eq(OfferStatus.CREATED), any(OffsetDateTime.class)))
          .thenReturn(expiredIds);
      when(offerRepository.findAssignmentReferencesByIds(expiredIds))
          .thenReturn(List.of(firstReference, secondReference));
      when(offerMapper.toExpiredTrigger(firstReference)).thenReturn(firstTrigger);
      when(offerMapper.toExpiredTrigger(secondReference)).thenReturn(secondTrigger);

      service.processExpired();

      verify(applicationEventPublisher).publishEvent(firstTrigger);
      verify(applicationEventPublisher).publishEvent(secondTrigger);
    }

    @Test
    @DisplayName("should clear execution context after processing")
    void shouldClearExecutionContextAfterProcessing() {
      UUID expiredId = UUID.randomUUID();
      when(offerRepository.findIdsByStatusAndExpiresAtBefore(
              eq(OfferStatus.CREATED), any(OffsetDateTime.class)))
          .thenReturn(List.of(expiredId));
      when(offerRepository.findAssignmentReferencesByIds(List.of(expiredId))).thenReturn(List.of());

      service.processExpired();

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should clear execution context even when an exception is thrown")
    void shouldClearExecutionContextEvenWhenExceptionThrown() {
      UUID expiredId = UUID.randomUUID();
      when(offerRepository.findIdsByStatusAndExpiresAtBefore(
              eq(OfferStatus.CREATED), any(OffsetDateTime.class)))
          .thenReturn(List.of(expiredId));
      when(offerRepository.findAssignmentReferencesByIds(any()))
          .thenThrow(new RuntimeException("db failure"));

      assertThatThrownBy(() -> service.processExpired()).isInstanceOf(RuntimeException.class);
      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }
}
