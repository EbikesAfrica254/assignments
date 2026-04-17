package com.ebikes.assignments.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import com.ebikes.assignments.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "offers", schema = "assignments")
public class Offer extends BaseEntity implements Auditable {

  @Column(name = "agent_id", nullable = false, length = 36)
  @NotNull private String agentId;

  @JoinColumn(name = "assignment_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private Assignment assignment;

  @Builder.Default
  @Column(name = "expiry_failure_count", nullable = false)
  private int expiryFailureCount = 0;

  @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  @NotNull private OffsetDateTime expiresAt;

  @Column(name = "responded_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime respondedAt;

  @Builder.Default
  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private OfferStatus status = OfferStatus.CREATED;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @Builder.Default
  @Column(name = "version", nullable = false)
  @Version
  private Long version = 0L;

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

  public boolean isExpired() {
    return OffsetDateTime.now(ZoneOffset.UTC).isAfter(this.expiresAt);
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("offerId", this.getId().toString());
    metadata.put("agentId", this.agentId);
    metadata.put("assignmentId", this.assignment.getId().toString());
    metadata.put("status", this.status.name());
    return Collections.unmodifiableMap(metadata);
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
