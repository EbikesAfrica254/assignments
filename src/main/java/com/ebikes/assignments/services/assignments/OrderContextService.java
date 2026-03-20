package com.ebikes.assignments.services.assignments;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.embeddables.PickupLocation;
import com.ebikes.assignments.database.repositories.OrderContextRepository;
import com.ebikes.assignments.dtos.events.incoming.OrderPendingAssignmentEvent;
import com.ebikes.assignments.dtos.events.incoming.OrderReassignmentRequestedEvent;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderContextService {

  private final OrderContextRepository orderContextRepository;

  @Transactional
  public void stage(OrderPendingAssignmentEvent event) {
    OrderContext context =
        new OrderContext(
            event.branchId(),
            event.committedQuoteId(),
            false,
            event.orderId(),
            event.organizationId(),
            new PickupLocation(event.pickupLatitude(), event.pickupLongitude()),
            event.vehicleClass());

    orderContextRepository.save(context);

    log.info("OrderContext staged for assignment: orderId={}", event.orderId());
  }

  @Transactional
  public void stage(OrderReassignmentRequestedEvent event) {
    OrderContext context =
        new OrderContext(
            event.branchId(),
            event.committedQuoteId(),
            true,
            event.orderId(),
            event.organizationId(),
            new PickupLocation(event.pickupLatitude(), event.pickupLongitude()),
            event.vehicleClass());

    orderContextRepository.save(context);

    log.info("OrderContext staged for reassignment: orderId={}", event.orderId());
  }

  @Transactional(readOnly = true)
  public OrderContext requireByOrderId(UUID orderId) {
    return orderContextRepository
        .findByOrderId(orderId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "OrderContext not staged for orderId: " + orderId));
  }
}
