package com.ebikes.assignments.listeners.external;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.ebikes.assignments.constants.EventConstants.ExternalContracts;
import com.ebikes.assignments.dtos.events.incoming.PendingAssignmentEvent;
import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.services.orders.OrderContextService;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.context.ExecutionContext;
import com.ebikes.assignments.support.fixtures.PendingAssignmentEventFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractListenerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("PendingAssignmentHandler")
class PendingAssignmentHandlerTest extends AbstractListenerTest {

  @Mock private InboxService inboxService;
  @Mock private OrderContextService orderContextService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private PendingAssignmentHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PendingAssignmentHandler(inboxService, objectMapper, orderContextService);
  }

  @Nested
  @DisplayName("handle")
  class Handle {

    @Test
    @DisplayName("should skip when event context is absent")
    void shouldSkipWhenEventContextIsAbsent() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();
      byte[] payload = objectMapper.writeValueAsBytes(event);

      handler.handle(payload);

      verify(inboxService, never()).receive(any(), any(), any());
    }

    @Test
    @DisplayName("should skip on duplicate event")
    void shouldSkipOnDuplicateEvent() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(false);

      handler.handle(payload);

      verify(orderContextService, never()).stage(any(PendingAssignmentEvent.class));
      verify(inboxService, never()).markProcessed(any());
    }

    @Test
    @DisplayName("should process event and mark processed")
    void shouldProcessEventAndMarkProcessed() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      handler.handle(payload);

      verify(orderContextService).stage(event);
      verify(inboxService).markProcessed(event.serviceReference());
    }

    @Test
    @DisplayName("should clear execution context after processing")
    void shouldClearExecutionContextAfterProcessing() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);

      handler.handle(payload);

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("should swallow exception when process throws")
    void shouldSwallowExceptionWhenProcessThrows() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      org.mockito.Mockito.doThrow(new RuntimeException("process failed"))
          .when(orderContextService)
          .stage(any(PendingAssignmentEvent.class));

      org.assertj.core.api.Assertions.assertThatCode(() -> handler.handle(payload))
          .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("should clear execution context when process throws")
    void shouldClearExecutionContextWhenProcessThrows() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();
      byte[] payload = objectMapper.writeValueAsBytes(event);
      EventContext.set("corr-id", "event-type", "routing-key", "source");
      when(inboxService.receive(any(), any(), any())).thenReturn(true);
      org.mockito.Mockito.doThrow(new RuntimeException("process failed"))
          .when(orderContextService)
          .stage(any(PendingAssignmentEvent.class));

      handler.handle(payload);

      assertThatThrownBy(ExecutionContext::get).isInstanceOf(IllegalStateException.class);
    }
  }

  @Nested
  @DisplayName("matches")
  class Matches {

    @Test
    @DisplayName("should match orders order entered pending assignment routing key")
    void shouldMatchOrdersOrderEnteredPendingAssignment() {
      org.assertj.core.api.Assertions.assertThat(
              handler.matches(ExternalContracts.ORDERS_ORDER_ENTERED_PENDING_ASSIGNMENT))
          .isTrue();
    }

    @Test
    @DisplayName("should not match unrelated routing key")
    void shouldNotMatchUnrelatedRoutingKey() {
      org.assertj.core.api.Assertions.assertThat(handler.matches("some.other.key")).isFalse();
    }
  }
}
