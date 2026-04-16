package com.ebikes.assignments.enums;

public enum OfferStatus {
  ACCEPTED,
  CANCELLED,
  CREATED,
  DECLINED,
  EXPIRED,
  EXPIRY_FAILED;

  public boolean isExhausted() {
    return this == ACCEPTED || this == DECLINED || this == EXPIRED || this == EXPIRY_FAILED;
  }

  public boolean isTerminal() {
    return this == ACCEPTED
        || this == CANCELLED
        || this == DECLINED
        || this == EXPIRED
        || this == EXPIRY_FAILED;
  }
}
