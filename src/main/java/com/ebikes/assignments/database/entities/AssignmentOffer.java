package com.ebikes.assignments.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import com.ebikes.assignments.database.entities.bases.BaseEntity;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.exceptions.BusinessRuleException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "assignment_offers", schema = "assignments")
public class AssignmentOffer extends BaseEntity {

  @Column(name = "agent_id", nullable = false, length = 36)
  @NotNull private String agentId;

  @JoinColumn(name = "assignment_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Assignment assignment;

  @Column(name = "expiry_failure_count", nullable = false)
  private int expiryFailureCount = 0;

  @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  @NotNull private OffsetDateTime expiresAt;

  @Column(name = "responded_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime respondedAt;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private OfferStatus status;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @Column(name = "version", nullable = false)
  @Version
  private Long version;

  AssignmentOffer(String agentId, Assignment assignment, OffsetDateTime expiresAt) {
    if (agentId == null || agentId.isBlank()) {
      throw new IllegalArgumentException("agentId is required");
    }
    if (assignment == null) {
      throw new IllegalArgumentException("assignment is required");
    }
    if (expiresAt == null) {
      throw new IllegalArgumentException("expiresAt is required");
    }
    if (!expiresAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
      throw new IllegalArgumentException("expiresAt must be in the future");
    }

    this.agentId = agentId;
    this.assignment = assignment;
    this.expiresAt = expiresAt;
    this.status = OfferStatus.CREATED;
    this.version = 0L;
  }

  public void accept() {
    guardNotTerminal();
    guardNotExpired();
    this.status = OfferStatus.ACCEPTED;
    this.respondedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void cancel() {
    guardNotTerminal();
    this.status = OfferStatus.CANCELLED;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void decline() {
    guardNotTerminal();
    guardNotExpired();
    this.status = OfferStatus.DECLINED;
    this.respondedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void expire() {
    guardNotTerminal();
    this.status = OfferStatus.EXPIRED;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void recordExpiryFailure(int maxAttempts) {
    if (this.status != OfferStatus.CREATED) {
      throw new IllegalStateException(
          "Cannot record expiry failure on offer not in CREATED state: " + getId());
    }
    this.expiryFailureCount++;
    if (this.expiryFailureCount >= maxAttempts) {
      this.status = OfferStatus.EXPIRY_FAILED;
    }
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public boolean isExpired() {
    return OffsetDateTime.now(ZoneOffset.UTC).isAfter(this.expiresAt);
  }

  private void guardNotExpired() {
    if (isExpired()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Offer has expired and can no longer be accepted or declined: " + getId());
    }
  }

  private void guardNotTerminal() {
    if (this.status.isTerminal()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Offer is already in a terminal state: " + this.status);
    }
  }
}
