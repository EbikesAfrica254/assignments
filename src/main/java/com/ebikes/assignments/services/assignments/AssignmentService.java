package com.ebikes.assignments.services.assignments;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.constants.EventConstants.DomainEvents;
import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.constants.EventConstants.Source;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentCancelledEvent;
import com.ebikes.assignments.dtos.events.outgoing.AssignmentSucceededEvent;
import com.ebikes.assignments.dtos.responses.assignments.AssignmentResponse;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.DuplicateResourceException;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.AssignmentMapper;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.services.orders.OrderContextService;
import com.ebikes.assignments.support.audit.AuditTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class AssignmentService {

  private final AssignmentMapper assignmentMapper;
  private final AssignmentRepository assignmentRepository;
  private final AuditTemplate auditTemplate;
  private final OfferService offerService;
  private final OrderContextService orderContextService;
  private final OutboxService outboxService;

  @Transactional
  public AssignmentResponse cancel(UUID assignmentId, String reason) {
    Assignment assignment =
        assignmentRepository
            .findByIdWithPessimisticLock(assignmentId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));

    offerService.cancelActiveOffers(assignment);

    auditTemplate.execute(
        assignment,
        assignment.getOrderContext().getOrganizationId(),
        DomainEvents.Assignments.CANCELLED,
        () -> {
          assignment.cancel(reason);
          assignmentRepository.save(assignment);
        });

    outboxService.publish(
        DomainEvents.Assignments.CANCELLED,
        new AssignmentCancelledEvent(
            assignment.getId(),
            reason,
            assignment.getOrderContext().getOrderId(),
            assignment.getOrderContext().getOrganizationId()),
        RoutingKeys.ASSIGNMENT_CANCELLED);

    log.info("Assignment cancelled: assignmentId={}, reason={}", assignmentId, reason);

    return assignmentMapper.toResponse(assignment);
  }

  @Transactional
  public Assignment create(OrderContext orderContext) {
    Assignment assignment = build(orderContext);
    return assignmentRepository.save(assignment);
  }

  @Transactional(readOnly = true)
  public AssignmentResponse getById(UUID assignmentId) {
    return assignmentMapper.toResponse(requireById(assignmentId));
  }

  @Transactional(readOnly = true)
  public AssignmentResponse getByOrderId(UUID orderId) {
    Assignment assignment =
        findByOrderId(orderId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ResponseCode.RESOURCE_NOT_FOUND,
                        "No assignment found for orderId: " + orderId));
    return assignmentMapper.toResponse(assignment);
  }

  @Transactional(readOnly = true)
  public boolean hasActiveAssignment(UUID orderId) {
    return assignmentRepository.hasActiveAssignmentForOrder(orderId);
  }

  @Transactional
  public AssignmentResponse manualAssign(UUID orderId, String agentId, String reason) {
    OrderContext orderContext = orderContextService.requireByOrderId(orderId);

    if (assignmentRepository.hasActiveAssignmentForOrder(orderId)) {
      throw new DuplicateResourceException(
          ResponseCode.INVALID_STATE,
          "An active assignment already exists for orderId: " + orderId);
    }

    Assignment assignment = build(orderContext);

    auditTemplate.execute(
        assignment,
        orderContext.getOrganizationId(),
        DomainEvents.Assignments.SUCCEEDED,
        () -> {
          assignment.setAssignmentReason(reason);
          assignment.succeed(agentId);
          assignmentRepository.save(assignment);
        });

    outboxService.publish(
        DomainEvents.Assignments.SUCCEEDED,
        new AssignmentSucceededEvent(
            assignment.getId(),
            orderContext.getOrderId(),
            orderContext.getOrganizationId(),
            Source.serviceReference(),
            agentId),
        RoutingKeys.ASSIGNMENT_SUCCEEDED);

    log.info(
        "Manual assignment completed: assignmentId={}, agentId={}", assignment.getId(), agentId);

    return assignmentMapper.toResponse(assignment);
  }

  @Transactional
  public void save(Assignment assignment) {
    assignmentRepository.save(assignment);
  }

  Optional<Assignment> findByOrderId(UUID orderId) {
    return assignmentRepository.findByOrderContextOrderId(orderId);
  }

  Assignment requireById(UUID assignmentId) {
    return assignmentRepository
        .findById(assignmentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Assignment not found: " + assignmentId));
  }

  private Assignment build(OrderContext orderContext) {
    return Assignment.builder()
        .orderContext(orderContext)
        .status(AssignmentStatus.AWAITING_RESPONSE)
        .strategy(AssignmentStrategy.PREASSIGNED)
        .build();
  }
}
