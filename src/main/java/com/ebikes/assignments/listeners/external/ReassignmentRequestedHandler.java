package com.ebikes.assignments.listeners.external;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.ExternalContracts;
import com.ebikes.assignments.dtos.events.incoming.ReassignmentRequestedEvent;
import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.services.orders.OrderContextService;

import tools.jackson.databind.ObjectMapper;

@Component
public class ReassignmentRequestedHandler
    extends AbstractIncomingEventHandler<ReassignmentRequestedEvent> {

  private final OrderContextService orderContextService;

  public ReassignmentRequestedHandler(
      InboxService inboxService,
      ObjectMapper objectMapper,
      OrderContextService orderContextService) {
    super(inboxService, objectMapper);
    this.orderContextService = orderContextService;
  }

  @Override
  protected Class<ReassignmentRequestedEvent> eventType() {
    return ReassignmentRequestedEvent.class;
  }

  @Override
  @Transactional
  protected void process(ReassignmentRequestedEvent event) {
    orderContextService.stage(event);
  }

  @Override
  protected String routingKey() {
    return ExternalContracts.ORDERS_ORDER_REASSIGNMENT_REQUESTED;
  }

  @Override
  protected String serviceReference(ReassignmentRequestedEvent event) {
    return event.serviceReference();
  }
}
