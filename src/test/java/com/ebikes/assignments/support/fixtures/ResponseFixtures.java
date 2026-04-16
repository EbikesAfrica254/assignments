package com.ebikes.assignments.support.fixtures;

import java.util.UUID;

import com.ebikes.assignments.dtos.responses.assignments.AssignmentResponse;
import com.ebikes.assignments.dtos.responses.offers.OfferResponse;
import com.ebikes.assignments.dtos.responses.outbox.OutboxResponse;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.enums.OutboxStatus;

import net.datafaker.Faker;

public final class ResponseFixtures {

  private static final Faker FAKER = new Faker();

  private ResponseFixtures() {}

  public static AssignmentResponse assignment() {
    return new AssignmentResponse(
        UUID.randomUUID(),
        null,
        null,
        null,
        UUID.randomUUID(),
        FAKER.internet().uuid(),
        AssignmentStatus.AWAITING_RESPONSE,
        AssignmentStrategy.PREASSIGNED,
        null,
        null);
  }

  public static OfferResponse offer() {
    return new OfferResponse(
        UUID.randomUUID(),
        FAKER.internet().uuid(),
        UUID.randomUUID(),
        null,
        null,
        null,
        OfferStatus.CREATED);
  }

  public static OutboxResponse outbox() {
    return new OutboxResponse(
        UUID.randomUUID(),
        null,
        FAKER.lorem().word(),
        0,
        FAKER.lorem().word(),
        OutboxStatus.PENDING,
        null);
  }
}
