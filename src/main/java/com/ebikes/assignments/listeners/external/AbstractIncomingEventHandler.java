package com.ebikes.assignments.listeners.external;

import com.ebikes.assignments.services.events.InboxService;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractIncomingEventHandler<T> {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;

  public final void handle(byte[] payload) {
    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing: routingKey={}", routingKey());
      return;
    }

    T event = objectMapper.readValue(payload, eventType());

    if (!inboxService.receive(
        EventContext.getEventType(), serviceReference(event), EventContext.getSourceService())) {
      return;
    }

    try {
      process(event);
      inboxService.markProcessed(serviceReference(event));
    } catch (Exception e) {
      log.error(
          "Failed to process event: routingKey={}, serviceReference={}",
          routingKey(),
          serviceReference(event),
          e);
    } finally {
      ExecutionContext.clear();
    }
  }

  public final boolean matches(String routingKey) {
    return routingKey().equals(routingKey);
  }

  protected abstract Class<T> eventType();

  protected abstract void process(T event);

  protected abstract String routingKey();

  protected abstract String serviceReference(T event);
}
