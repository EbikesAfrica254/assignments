package com.ebikes.assignments.dtos.events.internal;

import java.util.UUID;

public record OfferAcceptedTrigger(UUID assignmentId, String agentId) {}
