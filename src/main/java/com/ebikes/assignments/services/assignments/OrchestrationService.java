package com.ebikes.assignments.services.assignments;

import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.DomainEvents;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.dtos.events.internal.OfferAcceptedTrigger;
import com.ebikes.assignments.dtos.events.internal.OfferDeclinedTrigger;
import com.ebikes.assignments.dtos.events.internal.OfferExpiredTrigger;
import com.ebikes.assignments.dtos.events.internal.StrategyTrigger;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentFailedEvent;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentSucceededEvent;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.services.assignments.strategies.Strategy;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.services.orders.OrderContextService;
import com.ebikes.assignments.services.shortlist.ShortlistService;
import com.ebikes.assignments.support.audit.AuditTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrchestrationService {

  private static final String FAILURE_REASON_STRATEGIES_EXHAUSTED =
      "All configured strategies exhausted without a winner";

  private final ApplicationEventPublisher applicationEventPublisher;
  private final AssignmentService assignmentService;
  private final AuditTemplate auditTemplate;
  private final List<Strategy> executors;
  private final OfferService offerService;
  private final OrderContextService orderContextService;
  private final OutboxService outboxService;
  private final ShortlistService shortlistService;

  @Transactional
  public void executeNext(UUID assignmentId) {
    Assignment assignment = assignmentService.requireById(assignmentId);

    if (assignment.getStatus().isTerminal()) {
      log.warn("Assignment already terminal on executeNext: assignmentId={}", assignmentId);
      return;
    }

    execute(assignment, assignment.getOrderContext());
  }

  @Transactional
  public void onOfferAccepted(OfferAcceptedTrigger trigger) {
    Assignment assignment = assignmentService.requireById(trigger.assignmentId());

    if (assignment.getStatus().isTerminal()) {
      log.warn(
          "Assignment already terminal on offer accepted: assignmentId={}", trigger.assignmentId());
      return;
    }

    OrderContext orderContext = assignment.getOrderContext();

    auditTemplate.execute(
        assignment,
        orderContext.getOrganizationId(),
        DomainEvents.Assignments.SUCCEEDED,
        () -> {
          assignment.succeed(trigger.agentId());
          assignmentService.save(assignment);
        });

    outboxService.publish(
        DomainEvents.Assignments.SUCCEEDED,
        new AssignmentSucceededEvent(
            assignment.getId(),
            orderContext.getOrderId(),
            orderContext.getOrganizationId(),
            null,
            trigger.agentId()),
        RoutingKeys.ASSIGNMENT_SUCCEEDED);

    log.info(
        "Assignment succeeded: assignmentId={}, agentId={}",
        trigger.assignmentId(),
        trigger.agentId());
  }

  @Transactional
  public void onOfferDeclined(OfferDeclinedTrigger trigger) {
    progress(trigger.assignmentId(), trigger.strategy());
  }

  @Transactional
  public void onOfferExpired(OfferExpiredTrigger trigger) {
    progress(trigger.assignmentId(), trigger.strategy());
  }

  @Transactional
  public void start(AgentShortlistResolvedEvent event) {
    if (assignmentService.hasActiveAssignment(event.orderId())) {
      log.warn(
          "Active assignment already exists for orderId={}, ignoring shortlist", event.orderId());
      return;
    }

    OrderContext orderContext = orderContextService.requireByOrderId(event.orderId());
    Assignment assignment = assignmentService.create(orderContext);

    shortlistService.create(assignment, event.candidates());

    applicationEventPublisher.publishEvent(new StrategyTrigger(assignment.getId()));

    log.info(
        "Assignment started: assignmentId={}, orderId={}", assignment.getId(), event.orderId());
  }

  private void advance(Assignment assignment) {
    AssignmentStrategy next = assignment.getStrategy().next();

    if (next == null) {
      fail(assignment);
      return;
    }

    log.info(
        "Strategy exhausted, advancing: assignmentId={}, from={}, to={}",
        assignment.getId(),
        assignment.getStrategy(),
        next);

    assignment.updateStrategy(next);
    assignmentService.save(assignment);

    applicationEventPublisher.publishEvent(new StrategyTrigger(assignment.getId()));
  }

  private void execute(Assignment assignment, OrderContext orderContext) {
    Strategy executor = resolveExecutor(assignment.getStrategy());
    boolean placed = executor.execute(assignment, orderContext);

    if (!placed) {
      advance(assignment);
    }
  }

  private void fail(Assignment assignment) {
    OrderContext orderContext = assignment.getOrderContext();

    auditTemplate.execute(
        assignment,
        orderContext.getOrganizationId(),
        DomainEvents.Assignments.FAILED,
        () -> {
          assignment.fail(FAILURE_REASON_STRATEGIES_EXHAUSTED);
          assignmentService.save(assignment);
        });

    outboxService.publish(
        DomainEvents.Assignments.FAILED,
        new AssignmentFailedEvent(
            assignment.getId(),
            FAILURE_REASON_STRATEGIES_EXHAUSTED,
            orderContext.getOrderId(),
            orderContext.getOrganizationId()),
        RoutingKeys.ASSIGNMENT_FAILED);

    log.info(
        "Assignment failed: assignmentId={}, reason={}",
        assignment.getId(),
        FAILURE_REASON_STRATEGIES_EXHAUSTED);
  }

  private void progress(UUID assignmentId, AssignmentStrategy strategy) {
    Assignment assignment = assignmentService.requireById(assignmentId);

    if (assignment.getStatus().isTerminal()) {
      log.warn("Assignment already terminal on offer progressed: assignmentId={}", assignmentId);
      return;
    }

    if (strategy == AssignmentStrategy.BROADCAST && offerService.hasLiveOffer(assignmentId)) {
      log.debug("Broadcast has live offers, awaiting responses: assignmentId={}", assignmentId);
      return;
    }

    execute(assignment, assignment.getOrderContext());
  }

  private Strategy resolveExecutor(AssignmentStrategy strategy) {
    return executors.stream()
        .filter(e -> e.supports() == strategy)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("No executor registered for strategy: " + strategy));
  }
}
