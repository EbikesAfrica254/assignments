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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.assignments.database.entities.bases.BaseEntity;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
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
@Table(name = "assignments", schema = "assignments")
public class Assignment extends BaseEntity implements Auditable {

  @Column(name = "assignment_reason", length = 500)
  private String assignmentReason;

  @Column(name = "cancellation_reason", length = 500)
  private String cancellationReason;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @JoinColumn(name = "order_context_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private OrderContext orderContext;

  @Builder.Default
  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AssignmentStatus status = AssignmentStatus.AWAITING_RESPONSE;

  @Column(name = "strategy", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AssignmentStrategy strategy;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @Builder.Default
  @Column(name = "version", nullable = false)
  @Version
  private Long version = 0L;

  @Column(name = "winner_agent_id", length = 36)
  private String winnerAgentId;

  public void setAssignmentReason(String assignmentReason) {
    statusNotTerminal();
    this.assignmentReason = assignmentReason;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void cancel(String reason) {
    if (this.status.isTerminal()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot cancel an assignment already in a terminal state: " + this.status);
    }
    this.status = AssignmentStatus.CANCELLED;
    this.cancellationReason = reason;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void fail(String reason) {
    if (this.status != AssignmentStatus.AWAITING_RESPONSE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Can only transition to FAILED from AWAITING_RESPONSE. Current status: " + this.status);
    }
    this.status = AssignmentStatus.FAILED;
    this.failureReason = reason;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void statusNotTerminal() {
    if (this.status.isTerminal()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Assignment is in a terminal state and cannot be modified: " + this.status);
    }
  }

  public void succeed(@NotBlank String winnerAgentId) {
    if (this.status != AssignmentStatus.AWAITING_RESPONSE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Can only transition to SUCCEEDED from AWAITING_RESPONSE. Current status: "
              + this.status);
    }
    this.status = AssignmentStatus.SUCCEEDED;
    this.winnerAgentId = winnerAgentId;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("assignmentId", this.getId().toString());
    metadata.put("orderId", this.getOrderContext().getOrderId().toString());
    metadata.put("organizationId", this.getOrderContext().getOrganizationId());
    metadata.put("status", this.getStatus().name());
    metadata.put("strategy", this.getStrategy().name());
    if (this.getOrderContext().getBranchId() != null) {
      metadata.put("branchId", this.getOrderContext().getBranchId());
    }
    if (this.getWinnerAgentId() != null) {
      metadata.put("winnerAgentId", this.getWinnerAgentId());
    }
    if (this.getFailureReason() != null) {
      metadata.put("failureReason", this.getFailureReason());
    }
    if (this.getCancellationReason() != null) {
      metadata.put("cancellationReason", this.getCancellationReason());
    }
    return Collections.unmodifiableMap(metadata);
  }

  public void updateStrategy(AssignmentStrategy nextStrategy) {
    statusNotTerminal();
    if (nextStrategy == null) {
      throw new IllegalArgumentException("nextStrategy is required");
    }
    this.strategy = nextStrategy;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
