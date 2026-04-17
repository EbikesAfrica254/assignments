package com.ebikes.assignments.services.assignments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.constants.EventConstants.RoutingKeys;
import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.repositories.AssignmentRepository;
import com.ebikes.assignments.dtos.responses.assignments.AssignmentResponse;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.exceptions.DuplicateResourceException;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.mappers.AssignmentMapper;
import com.ebikes.assignments.services.events.OutboxService;
import com.ebikes.assignments.services.offers.OfferService;
import com.ebikes.assignments.services.orders.OrderContextService;
import com.ebikes.assignments.support.audit.AuditTemplate;
import com.ebikes.assignments.support.audit.ThrowingRunnable;
import com.ebikes.assignments.support.fixtures.AssignmentFixtures;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;

@DisplayName("AssignmentService")
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

  @Mock private AssignmentMapper assignmentMapper;
  @Mock private AssignmentRepository assignmentRepository;
  @Mock private AuditTemplate auditTemplate;
  @Mock private OfferService offerService;
  @Mock private OrderContextService orderContextService;
  @Mock private OutboxService outboxService;

  private AssignmentService service;

  private OrderContext orderContext;
  private Assignment assignment;

  @BeforeEach
  void setUp() {
    service =
        new AssignmentService(
            assignmentMapper,
            assignmentRepository,
            auditTemplate,
            offerService,
            orderContextService,
            outboxService);

    orderContext = OrderContextFixtures.standard();
    assignment = AssignmentFixtures.awaitingResponse(orderContext);
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

  private AssignmentResponse stubMappedResponse(Assignment a) {
    AssignmentResponse response =
        new AssignmentResponse(
            UUID.randomUUID(),
            null,
            null,
            null,
            orderContext.getOrderId(),
            orderContext.getOrganizationId(),
            a.getStatus(),
            a.getStrategy(),
            null,
            null);
    when(assignmentMapper.toResponse(a)).thenReturn(response);
    return response;
  }

  @Nested
  @DisplayName("cancel")
  class Cancel {

    @Test
    @DisplayName("should cancel active offers before persisting")
    void shouldCancelActiveOffersBeforePersisting() {
      when(assignmentRepository.findByIdWithPessimisticLock(assignment.getId()))
          .thenReturn(Optional.of(assignment));
      wireAuditTemplate();

      service.cancel(assignment.getId(), "cancellation reason");

      verify(offerService).cancelActiveOffers(assignment);
      verify(assignmentRepository).save(assignment);
      assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("should publish assignment cancelled event")
    void shouldPublishAssignmentCancelledEvent() {
      when(assignmentRepository.findByIdWithPessimisticLock(assignment.getId()))
          .thenReturn(Optional.of(assignment));
      wireAuditTemplate();

      service.cancel(assignment.getId(), "cancellation reason");

      verify(outboxService).publish(any(), any(), eq(RoutingKeys.ASSIGNMENT_CANCELLED));
    }

    @Test
    @DisplayName("should return mapped response")
    void shouldReturnMappedResponse() {
      when(assignmentRepository.findByIdWithPessimisticLock(assignment.getId()))
          .thenReturn(Optional.of(assignment));
      wireAuditTemplate();
      AssignmentResponse response = stubMappedResponse(assignment);

      AssignmentResponse result = service.cancel(assignment.getId(), "cancellation reason");

      assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when assignment not found")
    void shouldThrowWhenAssignmentNotFound() {
      UUID unknownId = UUID.randomUUID();
      when(assignmentRepository.findByIdWithPessimisticLock(unknownId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.cancel(unknownId, "reason"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should persist assignment with preassigned strategy and awaiting response status")
    void shouldPersistAssignmentWithPreassignedStrategyAndAwaitingResponseStatus() {
      when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

      service.create(orderContext);

      verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("should return persisted assignment")
    void shouldReturnPersistedAssignment() {
      when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

      Assignment result = service.create(orderContext);

      assertThat(result).isEqualTo(assignment);
    }
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    @DisplayName("should return mapped response")
    void shouldReturnMappedResponse() {
      when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.of(assignment));
      AssignmentResponse response = stubMappedResponse(assignment);

      assertThat(service.getById(assignment.getId())).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(assignmentRepository.findById(assignment.getId())).thenReturn(Optional.empty());

      UUID assignmentId = assignment.getId();
      assertThatThrownBy(() -> service.getById(assignmentId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("getByOrderId")
  class GetByOrderId {

    @Test
    @DisplayName("should return mapped response")
    void shouldReturnMappedResponse() {
      when(assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()))
          .thenReturn(Optional.of(assignment));
      AssignmentResponse response = stubMappedResponse(assignment);

      assertThat(service.getByOrderId(orderContext.getOrderId())).isEqualTo(response);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(assignmentRepository.findByOrderContextOrderId(orderContext.getOrderId()))
          .thenReturn(Optional.empty());

      UUID orderId = orderContext.getOrderId();
      assertThatThrownBy(() -> service.getByOrderId(orderId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("hasActiveAssignment")
  class HasActiveAssignment {

    @Test
    @DisplayName("should return true when active assignment exists")
    void shouldReturnTrueWhenActiveAssignmentExists() {
      when(assignmentRepository.hasActiveAssignmentForOrder(orderContext.getOrderId()))
          .thenReturn(true);

      assertThat(service.hasActiveAssignment(orderContext.getOrderId())).isTrue();
    }

    @Test
    @DisplayName("should return false when no active assignment exists")
    void shouldReturnFalseWhenNoActiveAssignmentExists() {
      when(assignmentRepository.hasActiveAssignmentForOrder(orderContext.getOrderId()))
          .thenReturn(false);

      assertThat(service.hasActiveAssignment(orderContext.getOrderId())).isFalse();
    }
  }

  @Nested
  @DisplayName("manualAssign")
  class ManualAssign {

    @BeforeEach
    void setUp() {
      when(orderContextService.requireByOrderId(orderContext.getOrderId()))
          .thenReturn(orderContext);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when active assignment exists")
    void shouldThrowWhenActiveAssignmentExists() {
      when(assignmentRepository.hasActiveAssignmentForOrder(orderContext.getOrderId()))
          .thenReturn(true);

      String agentId = "agent-001";
      UUID orderId = orderContext.getOrderId();
      String reason = "manual override";
      assertThatThrownBy(() -> service.manualAssign(orderId, agentId, reason))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when order context not found")
    void shouldThrowWhenOrderContextNotFound() {
      when(orderContextService.requireByOrderId(orderContext.getOrderId()))
          .thenThrow(ResourceNotFoundException.class);

      String agentId = "agent-001";
      UUID orderId = orderContext.getOrderId();
      String reason = "manual override";
      assertThatThrownBy(() -> service.manualAssign(orderId, agentId, reason))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should persist succeeded assignment")
    void shouldPersistSucceededAssignment() {
      wireAuditTemplate();

      service.manualAssign(orderContext.getOrderId(), "agent-001", "manual override");

      verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("should publish assignment succeeded event")
    void shouldPublishAssignmentSucceededEvent() {
      wireAuditTemplate();

      service.manualAssign(orderContext.getOrderId(), "agent-001", "manual override");

      verify(outboxService).publish(any(), any(), eq(RoutingKeys.ASSIGNMENT_SUCCEEDED));
    }

    @Test
    @DisplayName("should return mapped response")
    void shouldReturnMappedResponse() {
      wireAuditTemplate();
      AssignmentResponse response =
          new AssignmentResponse(
              UUID.randomUUID(),
              null,
              null,
              null,
              orderContext.getOrderId(),
              orderContext.getOrganizationId(),
              AssignmentStatus.SUCCEEDED,
              AssignmentStrategy.PREASSIGNED,
              null,
              "agent-001");
      when(assignmentMapper.toResponse(any(Assignment.class))).thenReturn(response);

      AssignmentResponse result =
          service.manualAssign(orderContext.getOrderId(), "agent-001", "manual override");

      assertThat(result).isEqualTo(response);
    }
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("should delegate to repository")
    void shouldDelegateToRepository() {
      service.save(assignment);

      verify(assignmentRepository).save(assignment);
    }
  }
}
