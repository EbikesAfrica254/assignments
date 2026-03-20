package com.ebikes.assignments.database.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import com.ebikes.assignments.database.entities.bases.BaseEntity;
import com.ebikes.assignments.enums.AssignmentStatus;
import com.ebikes.assignments.enums.AssignmentStrategy;
import com.ebikes.assignments.enums.OfferStatus;
import com.ebikes.assignments.enums.ResponseCode;
import com.ebikes.assignments.enums.VehicleClass;
import com.ebikes.assignments.exceptions.BusinessRuleException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "assignments", schema = "assignments")
public class Assignment extends BaseEntity {

  @Column(name = "cancellation_reason")
  private String cancellationReason;

  @OneToMany(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private final List<AssignmentOffer> offers = new ArrayList<>();

  @OneToMany(
      mappedBy = "assignment",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private final List<ShortlistCandidate> shortlistCandidates = new ArrayList<>();

  @Column(name = "failure_reason")
  private String failureReason;

  @JoinColumn(name = "order_context_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private OrderContext orderContext;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AssignmentStatus status;

  @Column(name = "strategy", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private AssignmentStrategy strategy;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @Column(name = "version", nullable = false)
  @Version
  private Long version;

  @Column(name = "winner_agent_id", length = 36)
  private String winnerAgentId;

  public Assignment(@NotNull OrderContext orderContext, @NotNull AssignmentStrategy strategy) {

    this.orderContext = orderContext;
    this.strategy = strategy;
    this.status = AssignmentStatus.STARTED;
    this.version = 0L;
  }

  public AssignmentOffer createOffer(String agentId, OffsetDateTime expiresAt) {
    guardNotTerminal();
    if (agentId == null || agentId.isBlank()) {
      throw new IllegalArgumentException("agentId is required");
    }

    boolean alreadyTerminal =
        this.offers.stream()
            .anyMatch(
                o ->
                    o.getAgentId().equals(agentId)
                        && o.getStatus().isTerminal()
                        && o.getStatus() != OfferStatus.CANCELLED);
    if (alreadyTerminal) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Agent already reached a terminal offer result for this attempt: " + agentId);
    }

    AssignmentOffer offer = new AssignmentOffer(agentId, this, expiresAt);
    this.offers.add(offer);
    return offer;
  }

  public void awaitResponse() {
    if (this.status != AssignmentStatus.STARTED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Can only transition to AWAITING_RESPONSE from STARTED. Current status: " + this.status);
    }
    this.status = AssignmentStatus.AWAITING_RESPONSE;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void succeed(String winnerAgentId) {
    if (this.status != AssignmentStatus.AWAITING_RESPONSE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Can only transition to SUCCEEDED from AWAITING_RESPONSE. Current status: "
              + this.status);
    }
    if (winnerAgentId == null || winnerAgentId.isBlank()) {
      throw new IllegalArgumentException("winnerAgentId is required");
    }

    this.status = AssignmentStatus.SUCCEEDED;
    this.winnerAgentId = winnerAgentId;
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

  public List<AssignmentOffer> cancel(String reason) {
    if (this.status.isTerminal()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Cannot cancel an assignment already in a terminal state: " + this.status);
    }

    List<AssignmentOffer> cancelled =
        this.offers.stream().filter(o -> !o.getStatus().isTerminal()).toList();

    cancelled.forEach(AssignmentOffer::cancel);

    this.status = AssignmentStatus.CANCELLED;
    this.cancellationReason = reason;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);

    return cancelled;
  }

  public void updateStrategy(AssignmentStrategy nextStrategy) {
    guardNotTerminal();
    if (nextStrategy == null) {
      throw new IllegalArgumentException("nextStrategy is required");
    }
    this.strategy = nextStrategy;
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public List<AssignmentOffer> getOffers() {
    return Collections.unmodifiableList(offers);
  }

  public void addCandidate(
      String agentId,
      boolean isPreferred,
      BigDecimal latitude,
      BigDecimal longitude,
      VehicleClass vehicleClass) {
    guardNotTerminal();
    ShortlistCandidate candidate =
        new ShortlistCandidate(agentId, this, isPreferred, latitude, longitude, vehicleClass);
    this.shortlistCandidates.add(candidate);
  }

  public void rankCandidates(List<String> agentIdsInRankOrder) {
    guardNotTerminal();
    if (agentIdsInRankOrder == null || agentIdsInRankOrder.isEmpty()) {
      throw new IllegalArgumentException("agentIdsInRankOrder must not be null or empty");
    }

    for (int i = 0; i < agentIdsInRankOrder.size(); i++) {
      String agentId = agentIdsInRankOrder.get(i);
      shortlistCandidates.stream()
          .filter(c -> c.getAgentId().equals(agentId))
          .findFirst()
          .orElseThrow(
              () ->
                  new IllegalArgumentException(
                      "Candidate not found in shortlist for agentId: " + agentId))
          .assignRank(i + 1);
    }
  }

  private void guardNotTerminal() {
    if (this.status.isTerminal()) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Assignment is in a terminal state and cannot be modified: " + this.status);
    }
  }
}
