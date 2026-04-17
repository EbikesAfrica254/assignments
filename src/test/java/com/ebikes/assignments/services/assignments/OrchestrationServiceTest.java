package com.ebikes.assignments.services.assignments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.dtos.events.internal.OfferAcceptedTrigger;
import com.ebikes.assignments.dtos.events.internal.OfferDeclinedTrigger;
import com.ebikes.assignments.dtos.events.internal.OfferExpiredTrigger;
import com.ebikes.assignments.dtos.events.internal.StrategyTrigger;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.services.assignments.strategies.Strategy;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.services.orders.OrderContextService;
import com.ebikes.assignments.services.shortlist.ShortlistService;
import com.ebikes.assignments.support.audit.AuditTemplate;
import com.ebikes.assignments.support.audit.ThrowingRunnable;
import com.ebikes.assignments.support.fixtures.AgentShortlistResolvedEventFixtures;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.CandidateFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;

@DisplayName("OrchestrationService")
@ExtendWith(MockitoExtension.class)
class OrchestrationServiceTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private AssignmentService assignmentService;
  @Mock private AuditTemplate auditTemplate;
  @Mock private OfferService offerService;
  @Mock private OrderContextService orderContextService;
  @Mock private OutboxService outboxService;
  @Mock private ShortlistService shortlistService;
  @Mock private Strategy executor;

  private OrchestrationService service;

  private OrderContext orderContext;
  private Assignment assignment;

  @BeforeEach
  void setUp() {
    service =
        new OrchestrationService(
            applicationEventPublisher,
            assignmentService,
            auditTemplate,
            List.of(executor),
            offerService,
            orderContextService,
            outboxService,
            shortlistService);

    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.PREASSIGNED);
  }

  @SuppressWarnings("unchecked")
  private void wireAuditTemplate() {
    doAnswer(
            invocation -> {
              ThrowingRunnable<?> operation = invocation.getArgument(3);
              operation.run();
              return null;
            })
        .when(auditTemplate)
        .execute(any(Assignment.class), anyString(), anyString(), any(ThrowingRunnable.class));
  }

  @Nested
  @DisplayName("start")
  class Start {

    @Test
    @DisplayName("should not create assignment when active assignment exists")
    void shouldNotCreateAssignmentWhenActiveAssignmentExists() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(CandidateFixtures.standard()));
      when(assignmentService.hasActiveAssignment(event.orderId())).thenReturn(true);

      service.start(event);

      verify(assignmentService, never()).create(any());
    }

    @Test
    @DisplayName(
        "should create assignment, shortlist, and publish trigger when no active assignment")
    void shouldCreateAssignmentShortlistAndPublishTrigger() {
      AgentShortlistResolvedEvent event =
          AgentShortlistResolvedEventFixtures.withOrderId(
              orderContext.getOrderId(), List.of(CandidateFixtures.standard()));
      when(assignmentService.hasActiveAssignment(event.orderId())).thenReturn(false);
      when(orderContextService.requireByOrderId(event.orderId())).thenReturn(orderContext);
      when(assignmentService.create(orderContext)).thenReturn(assignment);

      service.start(event);

      verify(assignmentService).create(orderContext);
      verify(shortlistService).create(assignment, event.candidates());
      verify(applicationEventPublisher).publishEvent(any(StrategyTrigger.class));
    }
  }

  @Nested
  @DisplayName("executeNext")
  class ExecuteNext {

    @Test
    @DisplayName("should not execute when assignment is terminal")
    void shouldNotExecuteWhenAssignmentIsTerminal() {
      Assignment terminal = AssignmentFixtures.succeeded(orderContext);
      when(assignmentService.requireById(terminal.getId())).thenReturn(terminal);

      service.executeNext(terminal.getId());

      verify(executor, never()).execute(any(), any());
    }

    @Test
    @DisplayName("should execute strategy when assignment is not terminal")
    void shouldExecuteStrategyWhenAssignmentIsNotTerminal() {
      when(executor.supports()).thenReturn(AssignmentStrategy.PREASSIGNED);
      when(assignmentService.requireById(assignment.getId())).thenReturn(assignment);
      when(executor.execute(assignment, orderContext)).thenReturn(true);

      service.executeNext(assignment.getId());

      verify(executor).execute(assignment, orderContext);
    }
  }

  @Nested
  @DisplayName("onOfferAccepted")
  class OnOfferAccepted {

    @Test
    @DisplayName("should not save when assignment is terminal")
    void shouldNotSaveWhenAssignmentIsTerminal() {
      Assignment terminal = AssignmentFixtures.succeeded(orderContext);
      OfferAcceptedTrigger trigger = new OfferAcceptedTrigger(terminal.getId(), "agent-001");
      when(assignmentService.requireById(terminal.getId())).thenReturn(terminal);

      service.onOfferAccepted(trigger);

      verify(assignmentService, never()).save(any());
    }

    @Test
    @DisplayName("should succeed assignment when offer accepted")
    void shouldSucceedAssignmentWhenOfferAccepted() {
      Assignment awaiting = AssignmentFixtures.awaitingResponse(orderContext);
      OfferAcceptedTrigger trigger = new OfferAcceptedTrigger(awaiting.getId(), "agent-001");
      when(assignmentService.requireById(awaiting.getId())).thenReturn(awaiting);
      wireAuditTemplate();

      service.onOfferAccepted(trigger);

      assertThat(awaiting.getStatus()).isEqualTo(AssignmentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("should publish assignment succeeded event when offer accepted")
    void shouldPublishAssignmentSucceededEventWhenOfferAccepted() {
      Assignment awaiting = AssignmentFixtures.awaitingResponse(orderContext);
      OfferAcceptedTrigger trigger = new OfferAcceptedTrigger(awaiting.getId(), "agent-001");
      when(assignmentService.requireById(awaiting.getId())).thenReturn(awaiting);
      wireAuditTemplate();

      service.onOfferAccepted(trigger);

      verify(outboxService).publish(any(), any(), eq(RoutingKeys.ASSIGNMENT_SUCCEEDED));
    }
  }

  @Nested
  @DisplayName("onOfferDeclined")
  class OnOfferDeclined {

    @Test
    @DisplayName("should not execute when assignment is terminal")
    void shouldNotExecuteWhenAssignmentIsTerminal() {
      Assignment terminal = AssignmentFixtures.succeeded(orderContext);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(terminal.getId(), AssignmentStrategy.PREASSIGNED);
      when(assignmentService.requireById(terminal.getId())).thenReturn(terminal);

      service.onOfferDeclined(trigger);

      verify(executor, never()).execute(any(), any());
    }

    @Test
    @DisplayName("should not execute when broadcast has live offers")
    void shouldNotExecuteWhenBroadcastHasLiveOffers() {
      Assignment broadcast =
          AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.BROADCAST);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(broadcast.getId(), AssignmentStrategy.BROADCAST);
      when(assignmentService.requireById(broadcast.getId())).thenReturn(broadcast);
      when(offerService.hasLiveOffer(broadcast.getId())).thenReturn(true);

      service.onOfferDeclined(trigger);

      verify(executor, never()).execute(any(), any());
    }

    @Test
    @DisplayName("should execute when broadcast has no live offers")
    void shouldExecuteWhenBroadcastHasNoLiveOffers() {
      Assignment broadcast =
          AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.BROADCAST);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(broadcast.getId(), AssignmentStrategy.BROADCAST);
      when(assignmentService.requireById(broadcast.getId())).thenReturn(broadcast);
      when(offerService.hasLiveOffer(broadcast.getId())).thenReturn(false);
      when(executor.supports()).thenReturn(AssignmentStrategy.BROADCAST);
      when(executor.execute(broadcast, orderContext)).thenReturn(true);

      service.onOfferDeclined(trigger);

      verify(executor).execute(broadcast, orderContext);
    }

    @Test
    @DisplayName("should execute when strategy is not broadcast")
    void shouldExecuteWhenStrategyIsNotBroadcast() {
      when(executor.supports()).thenReturn(AssignmentStrategy.PREASSIGNED);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(assignment.getId(), AssignmentStrategy.PREASSIGNED);
      when(assignmentService.requireById(assignment.getId())).thenReturn(assignment);
      when(executor.execute(assignment, orderContext)).thenReturn(true);

      service.onOfferDeclined(trigger);

      verify(executor).execute(assignment, orderContext);
    }
  }

  @Nested
  @DisplayName("onOfferExpired")
  class OnOfferExpired {

    @Test
    @DisplayName("should not execute when assignment is terminal")
    void shouldNotExecuteWhenAssignmentIsTerminal() {
      Assignment terminal = AssignmentFixtures.succeeded(orderContext);
      OfferExpiredTrigger trigger =
          new OfferExpiredTrigger(terminal.getId(), AssignmentStrategy.PREASSIGNED);
      when(assignmentService.requireById(terminal.getId())).thenReturn(terminal);

      service.onOfferExpired(trigger);

      verify(executor, never()).execute(any(), any());
    }

    @Test
    @DisplayName("should not execute when broadcast has live offers")
    void shouldNotExecuteWhenBroadcastHasLiveOffers() {
      Assignment broadcast =
          AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.BROADCAST);
      OfferExpiredTrigger trigger =
          new OfferExpiredTrigger(broadcast.getId(), AssignmentStrategy.BROADCAST);
      when(assignmentService.requireById(broadcast.getId())).thenReturn(broadcast);
      when(offerService.hasLiveOffer(broadcast.getId())).thenReturn(true);

      service.onOfferExpired(trigger);

      verify(executor, never()).execute(any(), any());
    }

    @Test
    @DisplayName("should execute when broadcast has no live offers")
    void shouldExecuteWhenBroadcastHasNoLiveOffers() {
      Assignment broadcast =
          AssignmentFixtures.withStrategy(orderContext, AssignmentStrategy.BROADCAST);
      OfferExpiredTrigger trigger =
          new OfferExpiredTrigger(broadcast.getId(), AssignmentStrategy.BROADCAST);
      when(assignmentService.requireById(broadcast.getId())).thenReturn(broadcast);
      when(offerService.hasLiveOffer(broadcast.getId())).thenReturn(false);
      when(executor.supports()).thenReturn(AssignmentStrategy.BROADCAST);
      when(executor.execute(broadcast, orderContext)).thenReturn(true);

      service.onOfferExpired(trigger);

      verify(executor).execute(broadcast, orderContext);
    }

    @Test
    @DisplayName("should execute when strategy is not broadcast")
    void shouldExecuteWhenStrategyIsNotBroadcast() {
      when(executor.supports()).thenReturn(AssignmentStrategy.PREASSIGNED);
      OfferExpiredTrigger trigger =
          new OfferExpiredTrigger(assignment.getId(), AssignmentStrategy.PREASSIGNED);
      when(assignmentService.requireById(assignment.getId())).thenReturn(assignment);
      when(executor.execute(assignment, orderContext)).thenReturn(true);

      service.onOfferExpired(trigger);

      verify(executor).execute(assignment, orderContext);
    }
  }

  @Nested
  @DisplayName("advance")
  class Advance {

    @Test
    @DisplayName("should update strategy and publish trigger when next strategy exists")
    void shouldUpdateStrategyAndPublishTriggerWhenNextStrategyExists() {
      when(executor.supports()).thenReturn(AssignmentStrategy.PREASSIGNED);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(assignment.getId(), AssignmentStrategy.PREASSIGNED);
      when(assignmentService.requireById(assignment.getId())).thenReturn(assignment);
      when(executor.execute(assignment, orderContext)).thenReturn(false);

      service.onOfferDeclined(trigger);

      assertThat(assignment.getStrategy()).isEqualTo(AssignmentStrategy.RANKED);
      verify(assignmentService).save(assignment);
      verify(applicationEventPublisher).publishEvent(any(StrategyTrigger.class));
    }

    @Test
    @DisplayName("should fail assignment and publish failed event when strategies exhausted")
    void shouldFailAssignmentAndPublishFailedEventWhenStrategiesExhausted() {
      Assignment broadcast =
          AssignmentFixtures.withStrategyAwaitingResponse(
              orderContext, AssignmentStrategy.BROADCAST);
      OfferDeclinedTrigger trigger =
          new OfferDeclinedTrigger(broadcast.getId(), AssignmentStrategy.BROADCAST);
      when(assignmentService.requireById(broadcast.getId())).thenReturn(broadcast);
      when(offerService.hasLiveOffer(broadcast.getId())).thenReturn(false);
      when(executor.supports()).thenReturn(AssignmentStrategy.BROADCAST);
      when(executor.execute(broadcast, orderContext)).thenReturn(false);
      wireAuditTemplate();

      service.onOfferDeclined(trigger);

      assertThat(broadcast.getStatus()).isEqualTo(AssignmentStatus.FAILED);
      verify(outboxService).publish(any(), any(), eq(RoutingKeys.ASSIGNMENT_FAILED));
    }
  }

  @Nested
  @DisplayName("resolveExecutor")
  class ResolveExecutor {

    @Test
    @DisplayName("should throw IllegalStateException when no executor matches strategy")
    void shouldThrowWhenNoExecutorMatchesStrategy() {
      when(executor.supports()).thenReturn(AssignmentStrategy.RANKED);
      when(assignmentService.requireById(assignment.getId())).thenReturn(assignment);

      UUID assignmentId = assignment.getId();
      assertThatThrownBy(() -> service.executeNext(assignmentId))
          .isInstanceOf(IllegalStateException.class);
    }
  }
}
