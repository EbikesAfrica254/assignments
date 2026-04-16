package com.ebikes.assignments.support.fixtures;

import java.math.BigDecimal;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Shortlist;
import com.ebikes.assignments.enums.VehicleClass;

import net.datafaker.Faker;

public final class ShortlistFixtures {

  private static final Faker FAKER = new Faker();

  private ShortlistFixtures() {}

  public static Shortlist preferred(Assignment assignment) {
    return base(assignment).isPreferred(true).build();
  }

  public static Shortlist ranked(Assignment assignment, int rank) {
    Shortlist shortlist = base(assignment).build();
    shortlist.assignRank(rank);
    return shortlist;
  }

  public static Shortlist unranked(Assignment assignment) {
    return base(assignment).build();
  }

  private static Shortlist.ShortlistBuilder<?, ?> base(Assignment assignment) {
    return Shortlist.builder()
        .agentId(FAKER.internet().uuid())
        .assignment(assignment)
        .isPreferred(false)
        .latitude(new BigDecimal(FAKER.address().latitude()))
        .longitude(new BigDecimal(FAKER.address().longitude()))
        .vehicleClass(VehicleClass.BICYCLE);
  }
}
