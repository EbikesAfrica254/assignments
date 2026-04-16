package com.ebikes.assignments.database.repositories;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.Assignment;
import com.ebikes.assignments.enums.AssignmentStatus;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

  Optional<Assignment> findByOrderContextOrderId(UUID orderId);

  Optional<Assignment> findByOrderContextIdAndStatusNotIn(
      UUID orderContextId, Collection<AssignmentStatus> statuses);

  @Query(
      "SELECT COUNT(a) > 0 FROM Assignment a WHERE a.orderContext.orderId = :orderId AND a.status"
          + " NOT IN ('SUCCEEDED', 'FAILED', 'CANCELLED')")
  boolean hasActiveAssignmentForOrder(@Param("orderId") UUID orderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Assignment a WHERE a.id = :id")
  Optional<Assignment> findByIdWithPessimisticLock(@Param("id") UUID id);
}
