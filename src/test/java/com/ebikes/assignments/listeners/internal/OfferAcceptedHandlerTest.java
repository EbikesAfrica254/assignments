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

import com.ebikes.assignments.dtos.events.internal.OfferAcceptedTrigger;
import com.ebikes.assignments.services.assignments.OrchestrationService;

@DisplayName("OfferAcceptedHandler")
@ExtendWith(MockitoExtension.class)
class OfferAcceptedHandlerTest {

  @Mock private OrchestrationService orchestrationService;

  private OfferAcceptedHandler handler;

  @BeforeEach
  void setUp() {
    handler = new OfferAcceptedHandler(orchestrationService);
  }

  @Nested
  @DisplayName("onOfferAccepted")
  class OnOfferAccepted {

    @Test
    @DisplayName("should delegate to orchestration service")
    void shouldDelegateToOrchestrationService() {
      OfferAcceptedTrigger trigger = new OfferAcceptedTrigger(UUID.randomUUID(), "agent-001");

      handler.onOfferAccepted(trigger);

      verify(orchestrationService).onOfferAccepted(trigger);
    }

    @Test
    @DisplayName("should swallow optimistic lock exception")
    void shouldSwallowOptimisticLockException() {
      OfferAcceptedTrigger trigger = new OfferAcceptedTrigger(UUID.randomUUID(), "agent-001");
      doThrow(new OptimisticLockException()).when(orchestrationService).onOfferAccepted(trigger);

      assertThatCode(() -> handler.onOfferAccepted(trigger)).doesNotThrowAnyException();
    }
  }
}
