package com.ebikes.assignments.dtos.internal;

public record MatrixEntry(
    String agentId, int distanceMeters, int durationSeconds, boolean isFallback) {}
