package com.ebikes.assignments.integration.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ebikes.assignments.adapters.routing.RoutingServiceAdapter;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.Offer;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.Outbox;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.database.repositories.OfferRepository;
import com.ebikes.assignments.database.repositories.OrderContextRepository;
import com.ebikes.assignments.database.repositories.OutboxRepository;
import com.ebikes.assignments.database.repositories.ShortlistRepository;
import com.ebikes.assignments.dtos.internal.MatrixEntry;
import com.ebikes.assignments.dtos.internal.MatrixResponse;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.services.assignments.OrchestrationService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.support.fixtures.AgentShortlistResolvedEventFixtures;
import com.ebikes.assignments.support.fixtures.CandidateFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractIntegrationTest;

@DisplayName("Assignment offer response")
class AssignmentOfferResponseIT extends AbstractIntegrationTest {

  @Autowired private AssignmentRepository assignmentRepository;
  @Autowired private OfferRepository offerRepository;
  @Autowired private OfferService offerService;
  @Autowired private OrderContextRepository orderContextRepository;
  @Autowired private OrchestrationService orchestrationService;
  @Autowired private OutboxRepository outboxRepository;
  @Autowired private ShortlistRepository shortlistRepository;

  @MockitoBean private RoutingServiceAdapter routingServiceAdapter;

  @AfterEach
  void tearDown() {
    outboxRepository.deleteAll();
    offerRepository.deleteAll();
    shortlistRepository.deleteAll();
    assignmentRepository.deleteAll();
    orderContextRepository.deleteAll();
  }

  private Assignment givenPreassignedAssignment() {
    OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
    orchestrationService.start(
        AgentShortlistResolvedEventFixtures.withOrderId(
            orderContext.getOrderId(), List.of(CandidateFixtures.preferred())));
    return assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
  }

  @Nested
  @DisplayName("accept offer")
  class AcceptOffer {

    @Test
    @DisplayName("should mark assignment SUCCEEDED with winner agent")
    void shouldMarkAssignmentSucceededWithWinnerAgent() {
      Assignment assignment = givenPreassignedAssignment();
      Offer offer = offerRepository.findByAssignmentId(assignment.getId()).getFirst();

      offerService.accept(offer.getId(), offer.getAgentId());

      Assignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
      assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.SUCCEEDED);
      assertThat(updated.getWinnerAgentId()).isEqualTo(offer.getAgentId());
    }

    @Test
    @DisplayName("should publish assignment succeeded event to outbox")
    void shouldPublishAssignmentSucceededEventToOutbox() {
      Assignment assignment = givenPreassignedAssignment();
      Offer offer = offerRepository.findByAssignmentId(assignment.getId()).getFirst();

      offerService.accept(offer.getId(), offer.getAgentId());

      var routingKeys = outboxRepository.findAll().stream().map(Outbox::getRoutingKey).toList();
      assertThat(routingKeys).contains(RoutingKeys.ASSIGNMENT_SUCCEEDED);
    }
  }

  @Nested
  @DisplayName("decline offer — advance to next strategy")
  class DeclineOfferAdvanceToNextStrategy {

    @Nested
    @DisplayName("when PREASSIGNED offer declined and RANKED candidate exists")
    class WhenPreassignedOfferDeclinedAndRankedCandidateExists {

      @Test
      @DisplayName("should advance to RANKED and create new offer")
      void shouldAdvanceToRankedAndCreateNewOffer() {
        var preferred = CandidateFixtures.preferred();
        var ranked = CandidateFixtures.standard();
        OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
        when(routingServiceAdapter.computeMatrix(any(), any()))
            .thenReturn(
                new MatrixResponse(
                    List.of(new MatrixEntry(ranked.agentId(), 500, 120, false)),
                    new BigDecimal("51.509865"),
                    new BigDecimal("-0.118092")));
        orchestrationService.start(
            AgentShortlistResolvedEventFixtures.withOrderId(
                orderContext.getOrderId(), List.of(preferred, ranked)));

        Assignment assignment =
            assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
        Offer preassigned = offerRepository.findByAssignmentId(assignment.getId()).getFirst();

        offerService.decline(preassigned.getId(), preassigned.getAgentId());

        Assignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(updated.getStrategy()).isEqualTo(AssignmentStrategy.RANKED);
        assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.AWAITING_RESPONSE);

        List<Offer> offers = offerRepository.findByAssignmentId(assignment.getId());
        assertThat(offers)
            .hasSize(2)
            .anyMatch(o -> o.getStatus() == OfferStatus.DECLINED)
            .anyMatch(o -> o.getStatus() == OfferStatus.CREATED);
      }
    }

    @Nested
    @DisplayName("when all RANKED candidates exhausted and BROADCAST candidates remain")
    class WhenAllRankedCandidatesExhaustedAndBroadcastCandidatesRemain {

      @Test
      @DisplayName("should advance to BROADCAST and create broadcast offers")
      void shouldAdvanceToBroadcastAndCreateBroadcastOffers() {
        var first = CandidateFixtures.standard();
        var second = CandidateFixtures.standard();
        var third = CandidateFixtures.standard();
        OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
        when(routingServiceAdapter.computeMatrix(any(), any()))
            .thenReturn(
                new MatrixResponse(
                    List.of(
                        new MatrixEntry(first.agentId(), 300, 60, false),
                        new MatrixEntry(second.agentId(), 400, 90, false)),
                    new BigDecimal("51.509865"),
                    new BigDecimal("-0.118092")));
        orchestrationService.start(
            AgentShortlistResolvedEventFixtures.withOrderId(
                orderContext.getOrderId(), List.of(first, second, third)));

        Assignment assignment =
            assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();

        Offer firstRanked =
            offerRepository.findByAssignmentId(assignment.getId()).stream()
                .filter(o -> o.getStatus() == OfferStatus.CREATED)
                .findFirst()
                .orElseThrow();
        offerService.decline(firstRanked.getId(), firstRanked.getAgentId());

        Offer secondRanked =
            offerRepository.findByAssignmentId(assignment.getId()).stream()
                .filter(o -> o.getStatus() == OfferStatus.CREATED)
                .findFirst()
                .orElseThrow();
        offerService.decline(secondRanked.getId(), secondRanked.getAgentId());

        Assignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
        assertThat(updated.getStrategy()).isEqualTo(AssignmentStrategy.BROADCAST);
        assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.AWAITING_RESPONSE);
        assertThat(offerRepository.findByAssignmentId(assignment.getId()))
            .anyMatch(o -> o.getStatus() == OfferStatus.CREATED);
      }
    }
  }

  @Nested
  @DisplayName("decline offer — strategies exhausted")
  class DeclineOfferStrategiesExhausted {

    @Test
    @DisplayName("should mark assignment FAILED")
    void shouldMarkAssignmentFailed() {
      var candidate = CandidateFixtures.standard();
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
      when(routingServiceAdapter.computeMatrix(any(), any()))
          .thenReturn(
              new MatrixResponse(
                  List.of(new MatrixEntry(candidate.agentId(), 300, 60, false)),
                  new BigDecimal("51.509865"),
                  new BigDecimal("-0.118092")));
      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(candidate)));

      Assignment assignment =
          assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
      Offer ranked = offerRepository.findByAssignmentId(assignment.getId()).getFirst();

      offerService.decline(ranked.getId(), ranked.getAgentId());

      Assignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
      assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.FAILED);
    }

    @Test
    @DisplayName("should publish assignment failed event to outbox")
    void shouldPublishAssignmentFailedEventToOutbox() {
      var candidate = CandidateFixtures.standard();
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
      when(routingServiceAdapter.computeMatrix(any(), any()))
          .thenReturn(
              new MatrixResponse(
                  List.of(new MatrixEntry(candidate.agentId(), 300, 60, false)),
                  new BigDecimal("51.509865"),
                  new BigDecimal("-0.118092")));
      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(candidate)));

      Assignment assignment =
          assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
      Offer ranked = offerRepository.findByAssignmentId(assignment.getId()).getFirst();

      offerService.decline(ranked.getId(), ranked.getAgentId());

      var routingKeys = outboxRepository.findAll().stream().map(Outbox::getRoutingKey).toList();
      assertThat(routingKeys).contains(RoutingKeys.ASSIGNMENT_FAILED);
    }
  }
}
