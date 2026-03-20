package com.ebikes.assignments.dtos.requests;

import java.util.UUID;

public record ManualAssignRequest(String agentId, UUID orderId, String reason) {}
