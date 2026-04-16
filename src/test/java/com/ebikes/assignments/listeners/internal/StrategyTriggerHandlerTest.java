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

import com.ebikes.assignments.dtos.events.internal.StrategyTrigger;
import com.ebikes.assignments.services.assignments.OrchestrationService;

@DisplayName("StrategyTriggerHandler")
@ExtendWith(MockitoExtension.class)
class StrategyTriggerHandlerTest {

  @Mock private OrchestrationService orchestrationService;

  private StrategyTriggerHandler handler;

  @BeforeEach
  void setUp() {
    handler = new StrategyTriggerHandler(orchestrationService);
  }

  @Nested
  @DisplayName("onStrategyAdvanced")
  class OnStrategyAdvanced {

    @Test
    @DisplayName("should delegate to orchestration service")
    void shouldDelegateToOrchestrationService() {
      StrategyTrigger trigger = new StrategyTrigger(UUID.randomUUID());

      handler.onStrategyAdvanced(trigger);

      verify(orchestrationService).executeNext(trigger.assignmentId());
    }

    @Test
    @DisplayName("should swallow optimistic lock exception")
    void shouldSwallowOptimisticLockException() {
      StrategyTrigger trigger = new StrategyTrigger(UUID.randomUUID());
      doThrow(new OptimisticLockException())
          .when(orchestrationService)
          .executeNext(trigger.assignmentId());

      assertThatCode(() -> handler.onStrategyAdvanced(trigger)).doesNotThrowAnyException();
    }
  }
}
