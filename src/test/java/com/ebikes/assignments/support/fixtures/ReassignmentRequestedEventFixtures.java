package com.ebikes.assignments.support.fixtures;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.assignments.dtos.events.incoming.ReassignmentRequestedEvent;
import com.ebikes.assignments.enums.VehicleClass;

import net.datafaker.Faker;

public final class ReassignmentRequestedEventFixtures {

  private static final Faker FAKER = new Faker();

  private ReassignmentRequestedEventFixtures() {}

  public static ReassignmentRequestedEvent standard() {
    return build(UUID.randomUUID(), VehicleClass.BICYCLE);
  }

  public static ReassignmentRequestedEvent withOrderId(UUID orderId) {
    return build(orderId, VehicleClass.BICYCLE);
  }

  public static ReassignmentRequestedEvent withVehicleClass(VehicleClass vehicleClass) {
    return build(UUID.randomUUID(), vehicleClass);
  }

  private static ReassignmentRequestedEvent build(UUID orderId, VehicleClass vehicleClass) {
    return new ReassignmentRequestedEvent(
        FAKER.internet().uuid(),
        UUID.randomUUID(),
        orderId,
        FAKER.internet().uuid(),
        new BigDecimal(FAKER.address().latitude()),
        new BigDecimal(FAKER.address().longitude()),
        "Reassignment requested by test",
        FAKER.internet().uuid(),
        vehicleClass);
  }
}
