package com.ebikes.assignments.dtos.events.outgoing;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssignmentSucceededEvent(
    UUID assignmentId,
    UUID orderId,
    String organizationId,
    String serviceReference,
    String winnerAgentId)
    implements Serializable {}
