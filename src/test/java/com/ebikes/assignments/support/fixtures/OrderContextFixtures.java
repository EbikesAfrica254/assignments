package com.ebikes.assignments.support.fixtures;

import java.math.BigDecimal;
import java.util.UUID;

import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.embeddables.PickupLocation;
import com.ebikes.assignments.enums.VehicleClass;

public final class OrderContextFixtures {

  private OrderContextFixtures() {}

  public static OrderContext reassignment() {
    return base().isReassignment(true).build();
  }

  public static OrderContext standard() {
    return base().build();
  }

  public static OrderContext withBranch() {
    return base().branchId("branch-001").build();
  }

  private static OrderContext.OrderContextBuilder<?, ?> base() {
    return OrderContext.builder()
        .committedQuoteId(UUID.randomUUID())
        .orderId(UUID.randomUUID())
        .organizationId("org-001")
        .pickupLocation(
            new PickupLocation(new BigDecimal("51.509865"), new BigDecimal("-0.118092")))
        .vehicleClass(VehicleClass.E_BIKE);
  }
}
