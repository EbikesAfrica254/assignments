package com.ebikes.assignments.support.events;

import org.springframework.messaging.Message;

import com.ebikes.assignments.constants.EventConstants.EventSource;
import com.ebikes.assignments.constants.EventConstants.MessageHeaders;
import com.ebikes.assignments.support.context.EventContext;
import com.ebikes.assignments.support.context.ExecutionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventContextDecorator {

  private EventContextDecorator() {
    // prevent instantiation
  }

  /**
   * Wraps a message handler with ExecutionContext (system identity) and EventContext (event
   * metadata) lifecycle management.
   */
  public static void decorate(Message<?> message, Runnable handler) {
    try {
      ExecutionContext.setSystem();
      EventContext.set(
          extractHeader(message, MessageHeaders.OUTBOX_ID),
          extractHeader(message, MessageHeaders.EVENT_TYPE),
          extractHeader(message, MessageHeaders.ROUTING_KEY),
          EventSource.HOST_SERVICE);

      handler.run();

    } finally {
      ExecutionContext.clear();
      EventContext.clear();
    }
  }

  private static String extractHeader(Message<?> message, String headerName) {
    Object value = message.getHeaders().get(headerName);
    return value instanceof String s ? s : null;
  }
}
