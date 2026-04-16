package com.ebikes.assignments.support.fixtures;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.dtos.internal.Candidate;

import net.datafaker.Faker;

public final class AgentShortlistResolvedEventFixtures {

  private static final Faker FAKER = new Faker();

  private AgentShortlistResolvedEventFixtures() {}

  public static AgentShortlistResolvedEvent emptyCandidates() {
    return build(UUID.randomUUID(), List.of(), OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
  }

  public static AgentShortlistResolvedEvent expired(List<Candidate> candidates) {
    return build(UUID.randomUUID(), candidates, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
  }

  public static AgentShortlistResolvedEvent expiredWithOrderId(
      UUID orderId, List<Candidate> candidates) {
    return build(orderId, candidates, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5));
  }

  public static AgentShortlistResolvedEvent standard(List<Candidate> candidates) {
    return build(UUID.randomUUID(), candidates, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
  }

  public static AgentShortlistResolvedEvent withOrderId(UUID orderId, List<Candidate> candidates) {
    return build(orderId, candidates, OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(30));
  }

  private static AgentShortlistResolvedEvent build(
      UUID orderId, List<Candidate> candidates, OffsetDateTime expiresAt) {
    return new AgentShortlistResolvedEvent(
        FAKER.internet().uuid(),
        candidates,
        expiresAt,
        orderId,
        FAKER.internet().uuid(),
        OffsetDateTime.now(ZoneOffset.UTC),
        FAKER.internet().uuid());
  }
}
