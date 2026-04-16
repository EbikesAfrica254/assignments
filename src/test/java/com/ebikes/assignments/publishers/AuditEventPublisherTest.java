package com.ebikes.assignments.publishers;

import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.constants.EventConstants.DomainEvents;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.dtos.events.outgoing.AuditEvent;
import com.ebikes.assignments.enums.AuditOutcome;
import com.ebikes.assignments.services.events.OutboxService;

@DisplayName("AuditEventPublisher")
@ExtendWith(MockitoExtension.class)
class AuditEventPublisherTest {

  private static final String EVENT_TYPE = DomainEvents.Offers.CREATED;
  private static final String ROUTING_KEY = RoutingKeys.ASSIGNMENT_OFFER_CREATED;

  @Mock private OutboxService outboxService;

  @InjectMocks private AuditEventPublisher publisher;

  private AuditEvent event() {
    return new AuditEvent(
        UUID.randomUUID(),
        "AGENT",
        EVENT_TYPE,
        null,
        null,
        Map.of("status", "ACTIVE"),
        UUID.randomUUID().toString(),
        AuditOutcome.SUCCESS,
        null,
        null,
        UUID.randomUUID().toString());
  }

  @Nested
  @DisplayName("publishSuccess")
  class PublishSuccess {

    @Test
    @DisplayName("should delegate to OutboxService with event type and routing key")
    void shouldDelegateToOutboxService() {
      AuditEvent auditEvent = event();

      publisher.publishSuccess(auditEvent, ROUTING_KEY);

      verify(outboxService).publish(EVENT_TYPE, auditEvent, ROUTING_KEY);
    }
  }

  @Nested
  @DisplayName("publishFailure")
  class PublishFailure {

    @Test
    @DisplayName("should delegate to OutboxService with event type and routing key")
    void shouldDelegateToOutboxService() {
      AuditEvent auditEvent = event();

      publisher.publishFailure(auditEvent, ROUTING_KEY);

      verify(outboxService).publish(EVENT_TYPE, auditEvent, ROUTING_KEY);
    }
  }
}
