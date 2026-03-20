package com.ebikes.assignments.dtos.internal;

import java.math.BigDecimal;
import java.util.List;

import com.ebikes.assignments.enums.VehicleClass;

public record MatrixRequestDto(
    List<MatrixAgentDto> agents,
    BigDecimal pickupLatitude,
    BigDecimal pickupLongitude,
    VehicleClass vehicleClass) {}
