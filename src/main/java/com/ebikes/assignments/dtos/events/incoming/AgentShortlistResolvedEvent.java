package com.ebikes.assignments.dtos.events.incoming;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.assignments.dtos.internal.Candidate;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentShortlistResolvedEvent(
    String branchId,
    List<Candidate> candidates,
    OffsetDateTime expiresAt,
    UUID orderId,
    String organizationId,
    OffsetDateTime resolvedAt,
    String serviceReference) {

  public AgentShortlistResolvedEvent {
    candidates = candidates == null ? null : List.copyOf(candidates);
  }
}
