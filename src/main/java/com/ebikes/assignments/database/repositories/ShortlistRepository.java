package com.ebikes.assignments.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.assignments.database.entities.Shortlist;

@Repository
public interface ShortlistRepository extends JpaRepository<Shortlist, UUID> {

  List<Shortlist> findByAssignmentIdAndRankIsNotNullOrderByRankAsc(UUID assignmentId);

  List<Shortlist> findByAssignmentIdOrderByRankAsc(UUID assignmentId);
}
