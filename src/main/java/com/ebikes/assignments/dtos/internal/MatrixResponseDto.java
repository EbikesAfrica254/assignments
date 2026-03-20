package com.ebikes.assignments.dtos.internal;

import java.math.BigDecimal;
import java.util.List;

public record MatrixResponseDto(
    List<MatrixEntryDto> entries, BigDecimal pickupLatitude, BigDecimal pickupLongitude) {}
