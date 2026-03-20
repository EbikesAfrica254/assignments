package com.ebikes.assignments.database.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.AssignmentOffer;
import com.ebikes.assignments.enums.OfferStatus;

@Repository
public interface AssignmentOfferRepository extends JpaRepository<AssignmentOffer, UUID> {

  List<AssignmentOffer> findByStatusAndExpiresAtBefore(
      OfferStatus status, OffsetDateTime threshold);
}
