package com.ebikes.assignments.database.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import com.ebikes.assignments.database.entities.bases.BaseEntity;
import com.ebikes.assignments.enums.VehicleClass;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "shortlist", schema = "assignments")
public class Shortlist extends BaseEntity {

  @Column(name = "agent_id", nullable = false, length = 36)
  @NotNull private String agentId;

  @JoinColumn(name = "assignment_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Assignment assignment;

  @Column(name = "is_preferred", nullable = false)
  private boolean isPreferred;

  @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
  @NotNull private BigDecimal latitude;

  @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
  @NotNull private BigDecimal longitude;

  @Column(name = "rank")
  private Integer rank;

  @Column(name = "vehicle_class", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private VehicleClass vehicleClass;

  public void assignRank(@NotNull int rank) {
    if (rank < 1) {
      throw new IllegalArgumentException("Rank must be a positive integer");
    }
    this.rank = rank;
  }
}
