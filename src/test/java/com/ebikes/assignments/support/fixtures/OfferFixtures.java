package com.ebikes.assignments.support.fixtures;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Offer;

public final class OfferFixtures {

  private OfferFixtures() {}

  public static Offer accepted(Assignment assignment) {
    Offer offer = base(assignment).build();
    offer.accept();
    return offer;
  }

  public static Offer cancelled(Assignment assignment) {
    Offer offer = base(assignment).build();
    offer.cancel();
    return offer;
  }

  public static Offer created(Assignment assignment) {
    return base(assignment).build();
  }

  public static Offer declined(Assignment assignment) {
    Offer offer = base(assignment).build();
    offer.decline();
    return offer;
  }

  public static Offer expired(Assignment assignment) {
    Offer offer = base(assignment).build();
    offer.expire();
    return offer;
  }

  public static Offer expiredByTime(Assignment assignment) {
    return base(assignment).expiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)).build();
  }

  private static Offer.OfferBuilder<?, ?> base(Assignment assignment) {
    return Offer.builder()
        .agentId("agent-001")
        .assignment(assignment)
        .expiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
  }
}
