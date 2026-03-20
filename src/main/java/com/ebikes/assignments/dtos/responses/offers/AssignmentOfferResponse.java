package com.ebikes.assignments.dtos.responses.offers;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.assignments.enums.OfferStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AssignmentOfferResponse(
    UUID id,
    String agentId,
    UUID assignmentId,
    OffsetDateTime createdAt,
    OffsetDateTime expiresAt,
    OffsetDateTime respondedAt,
    OfferStatus status) {}
