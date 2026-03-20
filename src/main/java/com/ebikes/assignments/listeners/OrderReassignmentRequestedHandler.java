package com.ebikes.assignments.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.assignments.services.assignments.OrderContextService;
import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReassignmentRequestedHandler implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;
  private final OrderContextService orderContextService;

  @Override
  @Transactional
  public void handle(byte[] payload) {
    OrderReassignmentRequestedEvent event =
        objectMapper.readValue(payload, OrderReassignmentRequestedEvent.class);

    log.debug("Received order reassignment requested event: orderId={}", event.orderId());
    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      orderContextService.stage(event);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process order reassignment requested event: orderId={}, serviceReference={}",
          event.orderId(),
          event.serviceReference(),
          e);
    } finally {
      ExecutionContext.clear();
    }
  }

  @Override
  public boolean matches(String assignmentsKey) {
    return RoutingKeys.ORDERS_ORDER_REASSIGNMENT_REQUESTED.equals(assignmentsKey);
  }
}
