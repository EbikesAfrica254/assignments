package com.ebikes.assignments.dtos.events.internal;

import java.util.UUID;

import com.ebikes.assignments.enums.AssignmentStrategy;

public record OfferExpiredTrigger(UUID assignmentId, AssignmentStrategy strategy) {}
