package com.ebikes.assignments.enums;

public enum AssignmentStrategy {
  BROADCAST,
  PREASSIGNED,
  RANKED;

  public AssignmentStrategy next() {
    return switch (this) {
      case PREASSIGNED -> RANKED;
      case RANKED -> BROADCAST;
      case BROADCAST -> null;
    };
  }
}
