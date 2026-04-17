package com.ebikes.assignments.support.fixtures;

import java.math.BigDecimal;

import com.ebikes.assignments.dtos.internal.Candidate;
import com.ebikes.assignments.enums.VehicleClass;

import net.datafaker.Faker;

public final class CandidateFixtures {

  private static final Faker FAKER = new Faker();

  private CandidateFixtures() {}

  public static Candidate preferred() {
    return new Candidate(
        FAKER.internet().uuid(),
        true,
        new BigDecimal(FAKER.address().latitude()),
        new BigDecimal(FAKER.address().longitude()),
        VehicleClass.BICYCLE);
  }

  public static Candidate standard() {
    return new Candidate(
        FAKER.internet().uuid(),
        false,
        new BigDecimal(FAKER.address().latitude()),
        new BigDecimal(FAKER.address().longitude()),
        VehicleClass.BICYCLE);
  }

  public static Candidate withVehicleClass(VehicleClass vehicleClass) {
    return new Candidate(
        FAKER.internet().uuid(),
        false,
        new BigDecimal(FAKER.address().latitude()),
        new BigDecimal(FAKER.address().longitude()),
        vehicleClass);
  }
}
