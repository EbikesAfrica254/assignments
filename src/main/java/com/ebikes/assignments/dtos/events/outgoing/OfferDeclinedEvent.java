package com.ebikes.assignments.dtos.events.outgoing;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OfferDeclinedEvent(String agentId, UUID assignmentId, UUID offerId)
    implements Serializable {}
