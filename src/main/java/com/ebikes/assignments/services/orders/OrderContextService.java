package com.ebikes.assignments.services.orders;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.assignments.database.entities.OrderContext;
import com.ebikes.assignments.database.entities.embeddables.PickupLocation;
import com.ebikes.assignments.database.repositories.OrderContextRepository;
import com.ebikes.assignments.dtos.events.incoming.PendingAssignmentEvent;
import com.ebikes.assignments.dtos.events.incoming.ReassignmentRequestedEvent;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.enums.VehicleClass;
import com.ebikes.assignments.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderContextService {

  private final OrderContextRepository orderContextRepository;

  @Transactional
  public void stage(PendingAssignmentEvent event) {
    stageContext(
        event.branchId(),
        event.committedQuoteId(),
        false,
        event.orderId(),
        event.organizationId(),
        new PickupLocation(event.pickupLatitude(), event.pickupLongitude()),
        event.vehicleClass());
  }

  @Transactional
  public void stage(ReassignmentRequestedEvent event) {
    stageContext(
        event.branchId(),
        event.committedQuoteId(),
        true,
        event.orderId(),
        event.organizationId(),
        new PickupLocation(event.pickupLatitude(), event.pickupLongitude()),
        event.vehicleClass());
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

  private void stageContext(
      String branchId,
      UUID committedQuoteId,
      boolean isReassignment,
      UUID orderId,
      String organizationId,
      PickupLocation pickupLocation,
      VehicleClass vehicleClass) {

    OrderContext context =
        OrderContext.builder()
            .branchId(branchId)
            .committedQuoteId(committedQuoteId)
            .isReassignment(isReassignment)
            .orderId(orderId)
            .organizationId(organizationId)
            .pickupLocation(pickupLocation)
            .vehicleClass(vehicleClass)
            .build();

    orderContextRepository.save(context);

    log.info("OrderContext staged: orderId={}, isReassignment={}", orderId, isReassignment);
  }
}
