package com.ebikes.assignments.integration.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.exceptions.BusinessRuleException;
import com.ebikes.assignments.services.assignments.AssignmentService;
import com.ebikes.assignments.services.assignments.OrchestrationService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.support.fixtures.AgentShortlistResolvedEventFixtures;
import com.ebikes.assignments.support.fixtures.CandidateFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractIntegrationTest;

@DisplayName("Assignment cancellation")
class AssignmentCancellationIT extends AbstractIntegrationTest {

  @Autowired private AssignmentRepository assignmentRepository;
  @Autowired private AssignmentService assignmentService;
  @Autowired private OfferRepository offerRepository;
  @Autowired private OfferService offerService;
  @Autowired private OrderContextRepository orderContextRepository;
  @Autowired private OrchestrationService orchestrationService;
  @Autowired private OutboxRepository outboxRepository;
  @Autowired private ShortlistRepository shortlistRepository;

  @AfterEach
  void tearDown() {
    outboxRepository.deleteAll();
    offerRepository.deleteAll();
    shortlistRepository.deleteAll();
    assignmentRepository.deleteAll();
    orderContextRepository.deleteAll();
  }

  private Assignment givenActiveAssignmentWithOffer() {
    OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
    orchestrationService.start(
        AgentShortlistResolvedEventFixtures.withOrderId(
            orderContext.getOrderId(), List.of(CandidateFixtures.preferred())));
    return assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
  }

  @Nested
  @DisplayName("cancel active assignment")
  class CancelActiveAssignment {

    @Test
    @DisplayName("should mark assignment CANCELLED with reason")
    void shouldMarkAssignmentCancelledWithReason() {
      Assignment assignment = givenActiveAssignmentWithOffer();

      assignmentService.cancel(assignment.getId(), "test reason");

      Assignment updated = assignmentRepository.findById(assignment.getId()).orElseThrow();
      assertThat(updated.getStatus()).isEqualTo(AssignmentStatus.CANCELLED);
      assertThat(updated.getCancellationReason()).isNotBlank();
    }

    @Test
    @DisplayName("should cancel all live offers")
    void shouldCancelAllLiveOffers() {
      Assignment assignment = givenActiveAssignmentWithOffer();

      assignmentService.cancel(assignment.getId(), "test reason");

      List<Offer> offers = offerRepository.findByAssignmentId(assignment.getId());
      assertThat(offers).isNotEmpty().allMatch(o -> o.getStatus() == OfferStatus.CANCELLED);
    }

    @Test
    @DisplayName("should publish cancellation events to outbox")
    void shouldPublishCancellationEventsToOutbox() {
      Assignment assignment = givenActiveAssignmentWithOffer();

      assignmentService.cancel(assignment.getId(), "test reason");

      var routingKeys = outboxRepository.findAll().stream().map(Outbox::getRoutingKey).toList();
      assertThat(routingKeys)
          .contains(RoutingKeys.ASSIGNMENT_CANCELLED)
          .contains(RoutingKeys.ASSIGNMENT_OFFER_CANCELLED);
    }
  }

  @Nested
  @DisplayName("cancel terminal assignment")
  class CancelTerminalAssignment {

    @Test
    @DisplayName("should throw when assignment is already terminal")
    void shouldThrowWhenAssignmentIsAlreadyTerminal() {
      Assignment assignment = givenActiveAssignmentWithOffer();
      Offer offer = offerRepository.findByAssignmentId(assignment.getId()).getFirst();
      offerService.accept(offer.getId(), offer.getAgentId());

      UUID assignmentId = assignment.getId();
      String reason = "test reason";
      assertThatThrownBy(() -> assignmentService.cancel(assignmentId, reason))
          .isInstanceOf(BusinessRuleException.class);
    }
  }
}
