package com.ebikes.assignments.dtos.events.outgoing;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfferCreatedEvent(
    String agentId, UUID assignmentId, OffsetDateTime expiresAt, UUID offerId)
    implements Serializable {}
