package com.ebikes.assignments.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.services.assignments.AssignmentOrchestrationService;
import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentShortlistResolvedHandler implements IncomingEventHandler {

  private final AssignmentOrchestrationService assignmentOrchestrationService;
  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
  public void handle(byte[] payload) {
    AgentShortlistResolvedEvent event =
        objectMapper.readValue(payload, AgentShortlistResolvedEvent.class);

    log.debug("Received agent shortlist resolved event: orderId={}", event.orderId());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }

    try {
      assignmentOrchestrationService.startOrchestration(event);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process agent shortlist resolved event: orderId={}, serviceReference={}",
          event.orderId(),
          event.serviceReference(),
          e);
    } finally {
      ExecutionContext.clear();
    }
  }

  @Override
  public boolean matches(String assignmentsKey) {
    return RoutingKeys.WORKFORCE_AGENT_SHORTLIST_RESOLVED.equals(assignmentsKey);
  }
}
