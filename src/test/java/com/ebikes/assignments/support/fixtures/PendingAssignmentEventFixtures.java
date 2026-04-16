package com.ebikes.assignments.support.fixtures;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.assignments.dtos.events.incoming.PendingAssignmentEvent;
import com.ebikes.assignments.enums.VehicleClass;

import net.datafaker.Faker;

public final class PendingAssignmentEventFixtures {

  private static final Faker FAKER = new Faker();

  private PendingAssignmentEventFixtures() {}

  public static PendingAssignmentEvent standard() {
    return build(UUID.randomUUID(), VehicleClass.BICYCLE);
  }

  public static PendingAssignmentEvent withOrderId(UUID orderId) {
    return build(orderId, VehicleClass.BICYCLE);
  }

  public static PendingAssignmentEvent withVehicleClass(VehicleClass vehicleClass) {
    return build(UUID.randomUUID(), vehicleClass);
  }

  private static PendingAssignmentEvent build(UUID orderId, VehicleClass vehicleClass) {
    return new PendingAssignmentEvent(
        FAKER.internet().uuid(),
        UUID.randomUUID(),
        orderId,
        FAKER.internet().uuid(),
        new BigDecimal(FAKER.address().latitude()),
        new BigDecimal(FAKER.address().longitude()),
        FAKER.internet().uuid(),
        vehicleClass);
  }
}
