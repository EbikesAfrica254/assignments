package com.ebikes.assignments.dtos.internal;

import java.util.UUID;

import com.ebikes.assignments.enums.AssignmentStrategy;

public record OfferTerminatedEvent(UUID assignmentId, AssignmentStrategy strategy) {}
