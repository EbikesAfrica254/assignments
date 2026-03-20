package com.ebikes.assignments.dtos.events.outgoing;

import java.io.Serializable;
import java.util.UUID;

import com.ebikes.assignments.enums.AssignmentStrategy;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssignmentStartedEvent(
    UUID assignmentId, UUID orderId, String organizationId, AssignmentStrategy strategy)
    implements Serializable {}
