package com.ebikes.assignments.services.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.repositories.OrderContextRepository;
import com.ebikes.assignments.dtos.events.incoming.PendingAssignmentEvent;
import com.ebikes.assignments.dtos.events.incoming.ReassignmentRequestedEvent;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;
import com.ebikes.assignments.support.fixtures.OrderContextFixtures;
import com.ebikes.assignments.support.fixtures.PendingAssignmentEventFixtures;
import com.ebikes.assignments.support.fixtures.ReassignmentRequestedEventFixtures;

@DisplayName("OrderContextService")
@ExtendWith(MockitoExtension.class)
class OrderContextServiceTest {

  @Mock private OrderContextRepository orderContextRepository;

  @InjectMocks private OrderContextService service;

  @Nested
  @DisplayName("stage(PendingAssignmentEvent)")
  class StageFromPendingAssignmentEvent {

    @Test
    @DisplayName("should persist order context from pending assignment event")
    void shouldPersistOrderContextFromPendingAssignmentEvent() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();

      service.stage(event);

      verify(orderContextRepository).save(any(OrderContext.class));
    }

    @Test
    @DisplayName("should map pending assignment event fields to order context")
    void shouldMapPendingAssignmentEventFieldsToOrderContext() {
      PendingAssignmentEvent event = PendingAssignmentEventFixtures.standard();

      service.stage(event);

      ArgumentCaptor<OrderContext> captor = ArgumentCaptor.forClass(OrderContext.class);
      verify(orderContextRepository).save(captor.capture());
      OrderContext saved = captor.getValue();
      assertThat(saved.getOrderId()).isEqualTo(event.orderId());
      assertThat(saved.getOrganizationId()).isEqualTo(event.organizationId());
      assertThat(saved.getBranchId()).isEqualTo(event.branchId());
      assertThat(saved.getCommittedQuoteId()).isEqualTo(event.committedQuoteId());
      assertThat(saved.getVehicleClass()).isEqualTo(event.vehicleClass());
      assertThat(saved.getPickupLocation().getLatitude())
          .isEqualByComparingTo(event.pickupLatitude());
      assertThat(saved.getPickupLocation().getLongitude())
          .isEqualByComparingTo(event.pickupLongitude());
      assertThat(saved.isReassignment()).isFalse();
    }
  }

  @Nested
  @DisplayName("stage(ReassignmentRequestedEvent)")
  class StageFromReassignmentRequestedEvent {

    @Test
    @DisplayName("should persist order context from reassignment requested event")
    void shouldPersistOrderContextFromReassignmentRequestedEvent() {
      ReassignmentRequestedEvent event = ReassignmentRequestedEventFixtures.standard();

      service.stage(event);

      verify(orderContextRepository).save(any(OrderContext.class));
    }

    @Test
    @DisplayName("should map reassignment requested event fields to order context")
    void shouldMapReassignmentRequestedEventFieldsToOrderContext() {
      ReassignmentRequestedEvent event = ReassignmentRequestedEventFixtures.standard();

      service.stage(event);

      ArgumentCaptor<OrderContext> captor = ArgumentCaptor.forClass(OrderContext.class);
      verify(orderContextRepository).save(captor.capture());
      OrderContext saved = captor.getValue();
      assertThat(saved.getOrderId()).isEqualTo(event.orderId());
      assertThat(saved.getOrganizationId()).isEqualTo(event.organizationId());
      assertThat(saved.getBranchId()).isEqualTo(event.branchId());
      assertThat(saved.getCommittedQuoteId()).isEqualTo(event.committedQuoteId());
      assertThat(saved.getVehicleClass()).isEqualTo(event.vehicleClass());
      assertThat(saved.getPickupLocation().getLatitude())
          .isEqualByComparingTo(event.pickupLatitude());
      assertThat(saved.getPickupLocation().getLongitude())
          .isEqualByComparingTo(event.pickupLongitude());
      assertThat(saved.isReassignment()).isTrue();
    }
  }

  @Nested
  @DisplayName("requireByOrderId")
  class RequireByOrderId {

    @Test
    @DisplayName("should return order context when found")
    void shouldReturnOrderContextWhenFound() {
      OrderContext orderContext = OrderContextFixtures.standard();
      when(orderContextRepository.findByOrderId(orderContext.getOrderId()))
          .thenReturn(Optional.of(orderContext));

      OrderContext result = service.requireByOrderId(orderContext.getOrderId());

      assertThat(result).isEqualTo(orderContext);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when order context not found")
    void shouldThrowResourceNotFoundExceptionWhenNotFound() {
      UUID orderId = UUID.randomUUID();
      when(orderContextRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.requireByOrderId(orderId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
