package com.ebikes.assignments.dtos.events.incoming;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.assignments.enums.VehicleClass;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReassignmentRequestedEvent(
    String branchId,
    UUID committedQuoteId,
    UUID orderId,
    String organizationId,
    BigDecimal pickupLatitude,
    BigDecimal pickupLongitude,
    String reason,
    String serviceReference,
    VehicleClass vehicleClass) {}
