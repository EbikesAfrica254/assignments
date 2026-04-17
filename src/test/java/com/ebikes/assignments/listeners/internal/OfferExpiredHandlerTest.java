package com.ebikes.assignments.listeners.internal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.dtos.events.internal.OfferExpiredTrigger;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.services.assignments.OrchestrationService;

@DisplayName("OfferExpiredHandler")
@ExtendWith(MockitoExtension.class)
class OfferExpiredHandlerTest {

  @Mock private OrchestrationService orchestrationService;

  private OfferExpiredHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OfferExpiredHandler(orchestrationService);
  }

  @Nested
  @DisplayName("onOfferExpired")
  class OnOfferExpired {

    @Test
    @DisplayName("should delegate to orchestration service")
    void shouldDelegateToOrchestrationService() {
      OfferExpiredTrigger trigger =
          new OfferExpiredTrigger(UUID.randomUUID(), AssignmentStrategy.PREASSIGNED);

      handler.onOfferExpired(trigger);

      verify(orchestrationService).onOfferExpired(trigger);
    }

    @Test
    @DisplayName("should swallow optimistic lock exception")
    void shouldSwallowOptimisticLockException() {
      OfferExpiredTrigger trigger =
          new OfferExpiredTrigger(UUID.randomUUID(), AssignmentStrategy.PREASSIGNED);
      doThrow(new OptimisticLockException()).when(orchestrationService).onOfferExpired(trigger);

      assertThatCode(() -> handler.onOfferExpired(trigger)).doesNotThrowAnyException();
    }
  }
}
