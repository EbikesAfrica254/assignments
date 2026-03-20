package com.ebikes.assignments.dtos.responses.assignments;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssignmentResponse(
    UUID id,
    String cancellationReason,
    OffsetDateTime createdAt,
    String failureReason,
    UUID orderId,
    String organizationId,
    AssignmentStatus status,
    AssignmentStrategy strategy,
    OffsetDateTime updatedAt,
    String winnerAgentId) {}
