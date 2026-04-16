package com.ebikes.assignments.database.projections;

import java.util.UUID;

import com.ebikes.assignments.enums.AssignmentStrategy;

public record AssignmentReference(UUID assignmentId, AssignmentStrategy strategy) {}
