package com.ebikes.assignments.integration.orchestration;

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
import com.ebikes.assignments.services.assignments.OrchestrationService;
import com.ebikes.assignments.support.fixtures.AgentShortlistResolvedEventFixtures;
import com.ebikes.assignments.support.fixtures.CandidateFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.infrastructure.AbstractIntegrationTest;

@DisplayName("Assignment start")
class AssignmentStartIT extends AbstractIntegrationTest {

  @Autowired private AssignmentRepository assignmentRepository;
  @Autowired private OfferRepository offerRepository;
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

  @Nested
  @DisplayName("when preferred candidate present")
  class WhenPreferredCandidatePresent {

    @Test
    @DisplayName("should create assignment in PREASSIGNED strategy awaiting response")
    void shouldCreateAssignmentInPreassignedStrategyAwaitingResponse() {
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());

      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(CandidateFixtures.preferred())));

      Assignment assignment =
          assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
      assertThat(assignment.getStrategy()).isEqualTo(AssignmentStrategy.PREASSIGNED);
      assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.AWAITING_RESPONSE);
    }

    @Test
    @DisplayName("should create one offer for preferred candidate")
    void shouldCreateOneOfferForPreferredCandidate() {
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());

      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(CandidateFixtures.preferred())));

      Assignment assignment =
          assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
      assertThat(offerRepository.findByAssignmentId(assignment.getId())).hasSize(1);
    }

    @Test
    @DisplayName("should publish offer created event to outbox")
    void shouldPublishOfferCreatedEventToOutbox() {
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());

      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(CandidateFixtures.preferred())));

      var routingKeys = outboxRepository.findAll().stream().map(Outbox::getRoutingKey).toList();
      assertThat(routingKeys).contains(RoutingKeys.ASSIGNMENT_OFFER_CREATED);
    }
  }

  @Nested
  @DisplayName("when no preferred candidate")
  class WhenNoPreferredCandidate {

    @Test
    @DisplayName("should create assignment in RANKED strategy awaiting response")
    void shouldCreateAssignmentInRankedStrategyAwaitingResponse() {
      var candidate = CandidateFixtures.standard();
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
      when(routingServiceAdapter.computeMatrix(any(), any()))
          .thenReturn(
              new MatrixResponse(
                  List.of(new MatrixEntry(candidate.agentId(), 500, 120, false)),
                  new BigDecimal("51.509865"),
                  new BigDecimal("-0.118092")));

      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(candidate)));

      Assignment assignment =
          assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
      assertThat(assignment.getStrategy()).isEqualTo(AssignmentStrategy.RANKED);
      assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.AWAITING_RESPONSE);
    }

    @Test
    @DisplayName("should create one offer for highest ranked candidate")
    void shouldCreateOneOfferForHighestRankedCandidate() {
      var candidate = CandidateFixtures.standard();
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
      when(routingServiceAdapter.computeMatrix(any(), any()))
          .thenReturn(
              new MatrixResponse(
                  List.of(new MatrixEntry(candidate.agentId(), 500, 120, false)),
                  new BigDecimal("51.509865"),
                  new BigDecimal("-0.118092")));

      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(candidate)));

      Assignment assignment =
          assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()).orElseThrow();
      assertThat(offerRepository.findByAssignmentId(assignment.getId())).hasSize(1);
    }

    @Test
    @DisplayName("should publish offer created event to outbox")
    void shouldPublishOfferCreatedEventToOutbox() {
      var candidate = CandidateFixtures.standard();
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
      when(routingServiceAdapter.computeMatrix(any(), any()))
          .thenReturn(
              new MatrixResponse(
                  List.of(new MatrixEntry(candidate.agentId(), 500, 120, false)),
                  new BigDecimal("51.509865"),
                  new BigDecimal("-0.118092")));

      orchestrationService.start(
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(candidate)));

      var routingKeys = outboxRepository.findAll().stream().map(Outbox::getRoutingKey).toList();
      assertThat(routingKeys).contains(RoutingKeys.ASSIGNMENT_OFFER_CREATED);
    }
  }

  @Nested
  @DisplayName("when active assignment already exists")
  class WhenActiveAssignmentAlreadyExists {

    @Test
    @DisplayName("should not create a second assignment")
    void shouldNotCreateSecondAssignment() {
      OrderContext orderContext = orderContextRepository.save(OrderContextFixtures.standard());
      var event =
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(CandidateFixtures.preferred()));

      orchestrationService.start(event);
      orchestrationService.start(event);

      assertThat(assignmentRepository.findAll()).hasSize(1);
    }
  }
}
