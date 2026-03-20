package com.ebikes.assignments.dtos.internal;

public record MatrixEntryDto(
    String agentId, int distanceMeters, int durationSeconds, boolean isFallback) {}
