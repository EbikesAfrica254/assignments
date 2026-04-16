package com.ebikes.assignments.dtos.internal;

import java.math.BigDecimal;
import java.util.List;

public record MatrixResponse(
    List<MatrixEntry> entries, BigDecimal pickupLatitude, BigDecimal pickupLongitude) {}
