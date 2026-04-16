package com.ebikes.assignments.database.entities;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.ebikes.assignments.database.entities.bases.BaseEntity;
import com.ebikes.assignments.database.entities.embeddables.PickupLocation;
import com.ebikes.assignments.enums.VehicleClass;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "order_contexts", schema = "assignments")
public class OrderContext extends BaseEntity {

  @Column(name = "branch_id", length = 36)
  private String branchId;

  @Column(name = "committed_quote_id", nullable = false)
  @NotNull private UUID committedQuoteId;

  @JsonProperty("isReassignment")
  @Column(name = "is_reassignment", nullable = false)
  private boolean isReassignment;

  @Column(name = "order_id", nullable = false)
  @NotNull private UUID orderId;

  @Column(name = "organization_id", nullable = false, length = 36)
  @NotNull private String organizationId;

  @Embedded @NotNull private PickupLocation pickupLocation;

  @Column(name = "vehicle_class", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private VehicleClass vehicleClass;
}
