package com.ebikes.assignments.listeners.external;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.configurations.properties.ServiceProperties;
import com.ebikes.assignments.constants.EventConstants.ExternalContracts;
import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.services.assignments.OrchestrationService;
import com.ebikes.assignments.services.events.InboxService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class ShortlistResolvedHandler
    extends AbstractIncomingEventHandler<AgentShortlistResolvedEvent> {

  private final OrchestrationService orchestrationService;
  private final ServiceProperties serviceProperties;

  public ShortlistResolvedHandler(
      InboxService inboxService,
      ObjectMapper objectMapper,
      OrchestrationService orchestrationService,
      ServiceProperties serviceProperties) {
    super(inboxService, objectMapper);
    this.orchestrationService = orchestrationService;
    this.serviceProperties = serviceProperties;
  }

  @Override
  protected Class<AgentShortlistResolvedEvent> eventType() {
    return AgentShortlistResolvedEvent.class;
  }

  @Override
  @Transactional
  protected void process(AgentShortlistResolvedEvent event) {
    if (event.candidates() == null || event.candidates().isEmpty()) {
      log.warn("Shortlist rejected — no candidates: orderId={}", event.orderId());
      return;
    }

    if (event.expiresAt() == null || event.expiresAt().isBefore(OffsetDateTime.now())) {
      log.warn(
          "Shortlist rejected — expired: orderId={}, expiresAt={}",
          event.orderId(),
          event.expiresAt());
      return;
    }

    int shortlistMax = serviceProperties.getCandidates().getShortlistMax();
    if (event.candidates().size() > shortlistMax) {
      log.warn(
          "Shortlist rejected — exceeds max: orderId={}, size={}, max={}",
          event.orderId(),
          event.candidates().size(),
          shortlistMax);
      return;
    }

    orchestrationService.start(event);
  }

  @Override
  protected String routingKey() {
    return ExternalContracts.WORKFORCE_AGENT_SHORTLIST_RESOLVED;
  }

  @Override
  protected String serviceReference(AgentShortlistResolvedEvent event) {
    return event.serviceReference();
  }
}
