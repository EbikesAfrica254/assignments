package com.ebikes.assignments.database.entities.embeddables;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PickupLocation implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Column(name = "pickup_latitude", nullable = false, precision = 9, scale = 6)
  @NotNull @DecimalMin("-90") @DecimalMax("90") private BigDecimal latitude;

  @Column(name = "pickup_longitude", nullable = false, precision = 9, scale = 6)
  @NotNull @DecimalMin("-180") @DecimalMax("180") private BigDecimal longitude;

  public PickupLocation(@NotNull BigDecimal latitude, @NotNull BigDecimal longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }
}
