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

import com.ebikes.assignments.dtos.events.internal.OfferDeclinedTrigger;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.services.assignments.OrchestrationService;

@DisplayName("OfferDeclinedHandler")
@ExtendWith(MockitoExtension.class)
class OfferDeclinedHandlerTest {

  @Mock private OrchestrationService orchestrationService;

  private OfferDeclinedHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OfferDeclinedHandler(orchestrationService);
  }

  @Nested
  @DisplayName("onOfferDeclined")
  class OnOfferDeclined {

    @Test
    @DisplayName("should delegate to orchestration service")
    void shouldDelegateToOrchestrationService() {
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(UUID.randomUUID(), AssignmentStrategy.PREASSIGNED);

      handler.onOfferDeclined(trigger);

      verify(orchestrationService).onOfferDeclined(trigger);
    }

    @Test
    @DisplayName("should swallow optimistic lock exception")
    void shouldSwallowOptimisticLockException() {
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(UUID.randomUUID(), AssignmentStrategy.PREASSIGNED);
      doThrow(new OptimisticLockException()).when(orchestrationService).onOfferDeclined(trigger);

      assertThatCode(() -> handler.onOfferDeclined(trigger)).doesNotThrowAnyException();
    }
  }
}
