package com.ebikes.assignments.services.assignments;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.EventSource;
import com.ebikes.assignments.constants.EventConstants.EventTypes;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.database.repositories.OrderContextRepository;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentStartedEvent;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentSucceededEvent;
import com.ebikes.assignments.dtos.events.outgoing.OfferCancelledEvent;
import com.ebikes.assignments.dtos.responses.assignments.AssignmentResponse;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.DuplicateResourceException;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.AssignmentMapper;
import com.ebikes.assignments.publishers.AuditEventPublisher;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.support.audit.AuditMetadataBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AssignmentService {

  private static final List<AssignmentStatus> TERMINAL_STATUSES =
      List.of(AssignmentStatus.SUCCEEDED, AssignmentStatus.FAILED, AssignmentStatus.CANCELLED);

  private final AssignmentMapper assignmentMapper;
  private final AssignmentRepository assignmentRepository;
  private final AuditEventPublisher auditEventPublisher;
  private final OrderContextRepository orderContextRepository;
  private final OutboxService outboxService;

  @Transactional
  public AssignmentResponse manualAssign(UUID orderId, String agentId, String reason) {
    log.info("Manual assignment initiated: orderId={}, agentId={}", orderId, agentId);

    OrderContext orderContext =
        orderContextRepository
            .findByOrderId(orderId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "OrderContext not found for orderId: " + orderId));

    assignmentRepository
        .findByOrderContextIdAndStatusNotIn(orderContext.getId(), TERMINAL_STATUSES)
        .ifPresent(
            a -> {
              throw new DuplicateResourceException(
                  ResponseCode.INVALID_STATE,
                  "An active assignment already exists for orderId: " + orderId);
            });

    Assignment assignment = new Assignment(orderContext, AssignmentStrategy.PREASSIGNED);
    assignment.awaitResponse();
    assignment.succeed(agentId);

    assignmentRepository.save(assignment);

    outboxService.save(
        EventTypes.Assignments.STARTED,
        new AssignmentStartedEvent(
            assignment.getId(),
            orderContext.getOrderId(),
            orderContext.getOrganizationId(),
            AssignmentStrategy.PREASSIGNED),
        RoutingKeys.ASSIGNMENT_STARTED);

    outboxService.save(
        EventTypes.Assignments.SUCCEEDED,
        new AssignmentSucceededEvent(
            assignment.getId(),
            orderContext.getOrderId(),
            orderContext.getOrganizationId(),
            EventSource.serviceReference(),
            agentId),
        RoutingKeys.ASSIGNMENT_SUCCEEDED);

    auditEventPublisher.publishSuccess(
        assignment.getId(),
        Assignment.class.getSimpleName(),
        EventTypes.Assignments.SUCCEEDED,
        AuditMetadataBuilder.forAssignment(assignment),
        orderContext.getOrganizationId(),
        RoutingKeys.ASSIGNMENT_AUDIT);

    log.info(
        "Manual assignment completed: assignmentId={}, agentId={}", assignment.getId(), agentId);

    return assignmentMapper.toResponse(assignment);
  }

  @Transactional
  public AssignmentResponse cancel(UUID assignmentId, String reason) {
    log.info("Cancelling assignment: assignmentId={}", assignmentId);

    Assignment assignment =
        assignmentRepository
            .findByIdWithPessimisticLock(assignmentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));

    List<AssignmentOffer> cancelledOffers = assignment.cancel(reason);
    assignmentRepository.save(assignment);

    cancelledOffers.forEach(
        offer ->
            outboxService.save(
                EventTypes.AssignmentOffers.CANCELLED,
                new OfferCancelledEvent(offer.getAgentId(), assignment.getId(), offer.getId()),
                RoutingKeys.ASSIGNMENT_OFFER_CANCELLED));

    auditEventPublisher.publishSuccess(
        assignment.getId(),
        Assignment.class.getSimpleName(),
        EventTypes.Assignments.CANCELLED,
        AuditMetadataBuilder.forAssignment(assignment),
        assignment.getOrderContext().getOrganizationId(),
        RoutingKeys.ASSIGNMENT_CANCELLED);

    return assignmentMapper.toResponse(assignment);
  }

  @Transactional(readOnly = true)
  public AssignmentResponse getById(UUID assignmentId) {
    return assignmentMapper.toResponse(requireById(assignmentId));
  }

  @Transactional(readOnly = true)
  public AssignmentResponse getByOrderId(UUID orderId) {
    Assignment assignment =
        assignmentRepository
            .findByOrderContextOrderId(orderId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "No assignment found for orderId: " + orderId));
    return assignmentMapper.toResponse(assignment);
  }

  Assignment requireById(UUID assignmentId) {
    return assignmentRepository
        .findById(assignmentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));
  }
}
