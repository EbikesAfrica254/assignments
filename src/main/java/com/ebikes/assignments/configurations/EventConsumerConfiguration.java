package com.ebikes.assignments.configurations;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import com.ebikes.assignments.listeners.external.AbstractIncomingEventHandler;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.events.EventContextDecorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class EventConsumerConfiguration {

  @Bean
  public Consumer<Message<?>> incomingEventConsumer(
      List<AbstractIncomingEventHandler<?>> handlers) {
    List<AbstractIncomingEventHandler<?>> immutable = List.copyOf(handlers);
    return message ->
        EventContextDecorator.decorate(
            message,
            () -> {
              String routingKey = EventContext.getRoutingKey();
              if (routingKey == null || routingKey.isBlank()) {
                log.warn("Received message with no routingKey header, discarding");
                return;
              }
              immutable.stream()
                  .filter(h -> h.matches(routingKey))
                  .findFirst()
                  .ifPresentOrElse(
                      h -> h.handle((byte[]) message.getPayload()),
                      () ->
                          log.warn(
                              "No handler registered for routingKey={}, discarding", routingKey));
            });
  }
}
