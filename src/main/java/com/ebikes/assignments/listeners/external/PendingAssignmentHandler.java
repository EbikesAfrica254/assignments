package com.ebikes.assignments.listeners.external;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.ExternalContracts;
import com.ebikes.assignments.dtos.events.incoming.PendingAssignmentEvent;
import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.services.orders.OrderContextService;

import tools.jackson.databind.ObjectMapper;

@Component
public class PendingAssignmentHandler extends AbstractIncomingEventHandler<PendingAssignmentEvent> {

  private final OrderContextService orderContextService;

  public PendingAssignmentHandler(
      InboxService inboxService,
      ObjectMapper objectMapper,
      OrderContextService orderContextService) {
    super(inboxService, objectMapper);
    this.orderContextService = orderContextService;
  }

  @Override
  protected Class<PendingAssignmentEvent> eventType() {
    return PendingAssignmentEvent.class;
  }

  @Override
  @Transactional
  protected void process(PendingAssignmentEvent event) {
    orderContextService.stage(event);
  }

  @Override
  protected String routingKey() {
    return ExternalContracts.ORDERS_ORDER_ENTERED_PENDING_ASSIGNMENT;
  }

  @Override
  protected String serviceReference(PendingAssignmentEvent event) {
    return event.serviceReference();
  }
}
