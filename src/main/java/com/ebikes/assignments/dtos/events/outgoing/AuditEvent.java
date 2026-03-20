package com.ebikes.assignments.dtos.events.outgoing;

import static com.ebikes.assignments.constants.EventConstants.EventSource;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.assignments.enums.AuditOutcome;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
    UUID entityId,
    @NotBlank String entityType,
    @NotBlank String eventType,
    String failureReason,
    String ipAddress,
    Map<String, String> metadata,
    String organizationId,
    @NotNull AuditOutcome outcome,
    String serviceReference,
    Instant timestamp,
    String userId)
    implements Serializable {

  public AuditEvent {
    if (eventType == null) {
      throw new IllegalArgumentException("eventType cannot be null");
    }
    if (outcome == null) {
      throw new IllegalArgumentException("outcome cannot be null");
    }

    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    serviceReference = serviceReference == null ? EventSource.serviceReference() : serviceReference;
    timestamp = timestamp == null ? Instant.now() : timestamp;
  }
}
