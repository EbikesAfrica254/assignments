package com.ebikes.assignments.enums;

public enum AssignmentStatus {
  AWAITING_RESPONSE,
  CANCELLED,
  FAILED,
  SUCCEEDED;

  public boolean isTerminal() {
    return this == CANCELLED || this == FAILED || this == SUCCEEDED;
  }
}
