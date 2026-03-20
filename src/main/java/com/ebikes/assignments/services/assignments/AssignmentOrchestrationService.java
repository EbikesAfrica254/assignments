package com.ebikes.assignments.services.assignments;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.ebikes.assignments.configurations.properties.AssignmentProperties;
import com.ebikes.assignments.constants.EventConstants.EventTypes;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.dtos.events.incoming.AgentShortlistResolvedEvent;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentFailedEvent;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentStartedEvent;
import com.ebikes.assignments.dtos.internal.OfferTerminatedEvent;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.publishers.AuditEventPublisher;
import com.ebikes.assignments.services.assignments.strategies.AssignmentStrategyExecutor;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.audit.AuditMetadataBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AssignmentOrchestrationService {

  private static final String FAILURE_REASON_STRATEGIES_EXHAUSTED =
      "All configured strategies exhausted without a winner";

  private final ApplicationEventPublisher applicationEventPublisher;
  private final AssignmentProperties assignmentProperties;
  private final AssignmentRepository assignmentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final List<AssignmentStrategyExecutor> executors;
  private final OrderContextService orderContextService;
  private final OutboxService outboxService;

  @Transactional
  public void startOrchestration(AgentShortlistResolvedEvent event) {
    if (event.candidates().isEmpty()) {
      log.warn("Empty shortlist received, cannot start orchestration: orderId={}", event.orderId());
      return;
    }

    if (!event.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
      log.warn(
          "Expired shortlist received, cannot start orchestration: orderId={}", event.orderId());
      return;
    }

    if (event.candidates().size() > assignmentProperties.getShortlistMaxCandidates()) {
      log.warn(
          "Shortlist exceeds max candidate cap of {}, cannot start orchestration: orderId={}",
          assignmentProperties.getShortlistMaxCandidates(),
          event.orderId());
      return;
    }

    OrderContext orderContext = orderContextService.requireByOrderId(event.orderId());

    boolean activeExists =
        assignmentRepository
            .findByOrderContextIdAndStatusNotIn(orderContext.getId(), getTerminalStatuses())
            .isPresent();

    if (activeExists) {
      log.warn(
          "Active assignment already exists for orderId={}, ignoring shortlist", event.orderId());
      return;
    }

    Assignment assignment = new Assignment(orderContext, AssignmentStrategy.PREASSIGNED);

    event
        .candidates()
        .forEach(
            c ->
                assignment.addCandidate(
                    c.agentId(), c.isPreferred(), c.latitude(), c.longitude(), c.vehicleClass()));

    assignmentRepository.save(assignment);

    outboxService.save(
        EventTypes.Assignments.STARTED,
        new AssignmentStartedEvent(
            assignment.getId(),
            orderContext.getOrderId(),
            orderContext.getOrganizationId(),
            assignment.getStrategy()),
        RoutingKeys.ASSIGNMENT_STARTED);

    auditEventPublisher.publishSuccess(
        assignment.getId(),
        Assignment.class.getSimpleName(),
        EventTypes.Assignments.STARTED,
        AuditMetadataBuilder.forAssignment(assignment),
        RoutingKeys.ASSIGNMENT_AUDIT);

    log.info(
        "Assignment started: assignmentId={}, orderId={}", assignment.getId(), event.orderId());

    executeCurrentStrategy(assignment, orderContext);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void progressOrchestration(OfferTerminatedEvent event) {
    Assignment assignment =
        assignmentRepository
            .findById(event.assignmentId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Assignment not found during orchestration progression: "
                            + event.assignmentId()));

    if (assignment.getStatus().isTerminal()) {
      return;
    }

    switch (assignment.getStrategy()) {
      case PREASSIGNED, RANKED -> executeCurrentStrategy(assignment, assignment.getOrderContext());
      case BROADCAST -> {
        boolean anyLive =
            assignment.getOffers().stream().anyMatch(o -> o.getStatus() == OfferStatus.CREATED);
        if (!anyLive) {
          executeCurrentStrategy(assignment, assignment.getOrderContext());
        }
      }
    }
  }

  public void publishOfferTerminated(UUID assignmentId, AssignmentStrategy strategy) {
    applicationEventPublisher.publishEvent(new OfferTerminatedEvent(assignmentId, strategy));
  }

  private void executeCurrentStrategy(Assignment assignment, OrderContext orderContext) {
    AssignmentStrategyExecutor executor =
        executors.stream()
            .filter(e -> e.supports() == assignment.getStrategy())
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No executor registered for strategy: " + assignment.getStrategy()));

    boolean placed = executor.execute(assignment, orderContext);

    if (!placed) {
      tryNextStrategy(assignment, orderContext);
    }
  }

  private void tryNextStrategy(Assignment assignment, OrderContext orderContext) {
    AssignmentStrategy next =
        switch (assignment.getStrategy()) {
          case PREASSIGNED -> AssignmentStrategy.RANKED;
          case RANKED -> AssignmentStrategy.BROADCAST;
          case BROADCAST -> null;
        };

    if (next == null) {
      failAssignment(assignment);
      return;
    }

    log.info(
        "Strategy exhausted, advancing: assignmentId={}, from={}, to={}",
        assignment.getId(),
        assignment.getStrategy(),
        next);

    assignment.updateStrategy(next);
    assignmentRepository.save(assignment);

    executeCurrentStrategy(assignment, orderContext);
  }

  private void failAssignment(Assignment assignment) {
    assignment.fail(FAILURE_REASON_STRATEGIES_EXHAUSTED);
    assignmentRepository.save(assignment);

    OrderContext ctx = assignment.getOrderContext();

    outboxService.save(
        EventTypes.Assignments.FAILED,
        new AssignmentFailedEvent(
            assignment.getId(),
            FAILURE_REASON_STRATEGIES_EXHAUSTED,
            ctx.getOrderId(),
            ctx.getOrganizationId()),
        RoutingKeys.ASSIGNMENT_FAILED);

    auditEventPublisher.publishFailure(
        assignment.getId(),
        Assignment.class.getSimpleName(),
        EventTypes.Assignments.FAILED,
        FAILURE_REASON_STRATEGIES_EXHAUSTED,
        AuditMetadataBuilder.forAssignment(assignment),
        ctx.getOrganizationId(),
        RoutingKeys.ASSIGNMENT_AUDIT);

    log.info(
        "Assignment failed: assignmentId={}, reason={}",
        assignment.getId(),
        FAILURE_REASON_STRATEGIES_EXHAUSTED);
  }

  private List<AssignmentStatus> getTerminalStatuses() {
    return List.of(AssignmentStatus.SUCCEEDED, AssignmentStatus.FAILED, AssignmentStatus.CANCELLED);
  }
}
