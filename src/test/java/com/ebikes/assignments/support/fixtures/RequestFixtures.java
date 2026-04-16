package com.ebikes.assignments.support.fixtures;

import java.util.UUID;

import com.ebikes.assignments.dtos.requests.CancelAssignmentRequest;
import com.ebikes.assignments.dtos.requests.ManualAssignRequest;
import com.ebikes.assignments.dtos.requests.filters.OutboxFilter;

import net.datafaker.Faker;

public final class RequestFixtures {

  private static final Faker FAKER = new Faker();

  private RequestFixtures() {}

  public static CancelAssignmentRequest cancel() {
    return new CancelAssignmentRequest(FAKER.lorem().sentence());
  }

  public static ManualAssignRequest manualAssign() {
    return new ManualAssignRequest(
        FAKER.internet().uuid(), UUID.randomUUID(), FAKER.lorem().sentence());
  }

  public static ManualAssignRequest manualAssignWithOrderId(UUID orderId) {
    return new ManualAssignRequest(FAKER.internet().uuid(), orderId, FAKER.lorem().sentence());
  }

  public static OutboxFilter outboxFilter() {
    return new OutboxFilter();
  }
}
