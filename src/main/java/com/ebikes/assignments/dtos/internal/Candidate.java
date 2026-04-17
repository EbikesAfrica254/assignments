package com.ebikes.assignments.dtos.internal;

import java.math.BigDecimal;

import com.ebikes.assignments.enums.VehicleClass;

public record Candidate(
    String agentId,
    boolean isPreferred,
    BigDecimal latitude,
    BigDecimal longitude,
    VehicleClass vehicleClass) {}
