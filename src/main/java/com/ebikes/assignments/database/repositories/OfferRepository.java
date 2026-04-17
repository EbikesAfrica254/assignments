package com.ebikes.assignments.database.repositories;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.Offer;
import com.ebikes.assignments.database.projections.AssignmentReference;
import com.ebikes.assignments.enums.OfferStatus;

@Repository
public interface OfferRepository extends JpaRepository<Offer, UUID> {

  @Modifying
  @Query("UPDATE Offer o SET o.status = :status WHERE o.id IN :ids")
  void bulkUpdateStatus(@Param("ids") Collection<UUID> ids, @Param("status") OfferStatus status);

  boolean existsByAssignmentIdAndStatus(UUID assignmentId, OfferStatus status);

  @Query(
      "SELECT new com.ebikes.assignments.database.projections.AssignmentReference(o.assignment.id,"
          + " o.assignment.strategy) FROM Offer o WHERE o.id IN :ids GROUP BY o.assignment.id,"
          + " o.assignment.strategy")
  List<AssignmentReference> findAssignmentReferencesByIds(@Param("ids") Collection<UUID> ids);

  List<Offer> findByAssignmentId(UUID assignmentId);

  @Query(
      "SELECT o.agentId FROM Offer o WHERE o.assignment.id = :assignmentId AND o.status IN"
          + " :exhaustedStatuses")
  Set<String> findExhaustedAgentIds(
      @Param("assignmentId") UUID assignmentId,
      @Param("exhaustedStatuses") Collection<OfferStatus> exhaustedStatuses);

  @Query("SELECT o.id FROM Offer o WHERE o.status = :status AND o.expiresAt < :threshold")
  List<UUID> findIdsByStatusAndExpiresAtBefore(
      @Param("status") OfferStatus status, @Param("threshold") OffsetDateTime threshold);
}
